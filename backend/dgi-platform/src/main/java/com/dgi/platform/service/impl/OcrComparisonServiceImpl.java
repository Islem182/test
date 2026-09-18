package com.dgi.platform.service.impl;

import com.dgi.platform.dto.FieldComparisonResult;
import com.dgi.platform.dto.OcrComparisonReport;
import com.dgi.platform.dto.OcrComparisonRequest;
import com.dgi.platform.service.ArabicLatinTransliterator;
import com.dgi.platform.service.OcrComparisonService;
import com.dgi.platform.service.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Per-field comparison engine for OCR identity verification.
 *
 * Strategy matrix
 * ───────────────
 * cinNumber / passportNumber  → EXACT   (no fuzzy — security requirement)
 * firstName / lastName        → FUZZY_NAME (normalize + transliterate + Levenshtein)
 * birthDate                   → DATE   (normalize format, then exact)
 * address                     → ADDRESS (token overlap with normalized tokens)
 * nationality                 → EXACT  (ISO code or country name, normalized)
 *
 * Score thresholds
 * ────────────────
 *   ≥ 95  → MATCH
 *   70–94 → POSSIBLE_MATCH
 *   50–69 → MANUAL_REVIEW
 *   < 50  → MISMATCH
 *
 * For EXACT strategies only MATCH (100) or MISMATCH (0) are produced.
 *
 * Overall score = weighted average (document-number & date fields weight 2×).
 * Overall status = worst status across all compared fields, except:
 *   - A single POSSIBLE_MATCH on a name field does NOT escalate to overall MISMATCH.
 *   - Any MISMATCH on document number or date → overall MISMATCH immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrComparisonServiceImpl implements OcrComparisonService {

    private final TextNormalizationService normalizer;
    private final ArabicLatinTransliterator transliterator;

    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();

    // ── Status constants ──────────────────────────────────────────────────────
    private static final String MATCH          = "MATCH";
    private static final String POSSIBLE_MATCH = "POSSIBLE_MATCH";
    private static final String MANUAL_REVIEW  = "MANUAL_REVIEW";
    private static final String MISMATCH       = "MISMATCH";

    // ── Strategy constants ────────────────────────────────────────────────────
    private static final String STRATEGY_EXACT       = "EXACT";
    private static final String STRATEGY_FUZZY_NAME  = "FUZZY_NAME";
    private static final String STRATEGY_DATE        = "DATE";
    private static final String STRATEGY_ADDRESS     = "ADDRESS";

    // ── Field labels ──────────────────────────────────────────────────────────
    private static final Map<String, String> LABELS = Map.of(
            "cinNumber",      "CIN Number / رقم بطاقة الهوية",
            "passportNumber", "Passport Number / رقم جواز السفر",
            "firstName",      "First Name / الاسم",
            "lastName",       "Last Name / اللقب",
            "birthDate",      "Date of Birth / تاريخ الميلاد",
            "address",        "Address / العنوان",
            "nationality",    "Nationality / الجنسية"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public OcrComparisonReport compareFields(OcrComparisonRequest request) {
        String docType = request.getDocumentType();
        Map<String, String> user = request.getUserEnteredData();
        Map<String, String> ocr  = request.getOcrExtractedData();

        // Determine which fields to compare for this document type
        List<String> fieldsToCompare = "PASSPORT".equalsIgnoreCase(docType)
                ? List.of("passportNumber", "firstName", "lastName", "birthDate", "nationality")
                : List.of("cinNumber", "firstName", "lastName", "birthDate", "address");

        Map<String, FieldComparisonResult> results = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();

        for (String field : fieldsToCompare) {
            String userVal = user.getOrDefault(field, "").trim();
            String ocrVal  = ocr.getOrDefault(field, "").trim();

            if (userVal.isEmpty() || ocrVal.isEmpty()) {
                skipped.add(field);
                log.debug("Comparison skipped for field={} — user='{}' ocr='{}'",
                        field, userVal.isEmpty() ? "<empty>" : "<present>",
                        ocrVal.isEmpty() ? "<empty>" : "<present>");
                continue;
            }

            FieldComparisonResult result = compareField(field, userVal, ocrVal);
            results.put(field, result);
            log.debug("Field comparison — field={}, strategy={}, status={}, score={}",
                    field, result.getStrategy(), result.getStatus(), result.getScore());
        }

        return buildReport(docType, results, skipped);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-field dispatch
    // ─────────────────────────────────────────────────────────────────────────

    private FieldComparisonResult compareField(String field, String userVal, String ocrVal) {
        return switch (field) {
            case "cinNumber", "passportNumber", "nationality" ->
                    compareExact(field, userVal, ocrVal);
            case "firstName", "lastName" ->
                    compareFuzzyName(field, userVal, ocrVal);
            case "birthDate" ->
                    compareDate(field, userVal, ocrVal);
            case "address" ->
                    compareAddress(field, userVal, ocrVal);
            default ->
                    compareExact(field, userVal, ocrVal);
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy: EXACT
    // Used for: cinNumber, passportNumber, nationality
    // ─────────────────────────────────────────────────────────────────────────

    private FieldComparisonResult compareExact(String field, String userVal, String ocrVal) {
        String normUser = normalizer.normalizeDocumentNumber(userVal);
        String normOcr  = normalizer.normalizeDocumentNumber(ocrVal);

        boolean match = normUser.equals(normOcr);
        int score = match ? 100 : 0;
        String status = match ? MATCH : MISMATCH;
        String message = match ? null
                : "Document number does not match. User entered: " + userVal
                  + " — OCR extracted: " + ocrVal
                  + ". An agent must verify the original document.";

        return FieldComparisonResult.builder()
                .fieldKey(field)
                .fieldLabel(LABELS.getOrDefault(field, field))
                .userValue(userVal)
                .ocrValue(ocrVal)
                .status(status)
                .score(score)
                .message(message)
                .strategy(STRATEGY_EXACT)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy: FUZZY_NAME
    // Used for: firstName, lastName
    //
    // Algorithm:
    //  1. Normalize both sides (diacritics, case, accents).
    //  2. If both are Latin → direct Levenshtein similarity.
    //  3. If one is Arabic and one is Latin → transliterate Arabic to Latin,
    //     also reverse-transliterate Latin to Arabic. Take the best of:
    //       a. similarity(arabicToLatin(arabicSide), normalizedLatinSide)
    //       b. similarity(normalizedArabicSide, latinToArabic(latinSide))
    //  4. If both are Arabic → direct Levenshtein on normalized Arabic.
    // ─────────────────────────────────────────────────────────────────────────

    private FieldComparisonResult compareFuzzyName(String field, String userVal, String ocrVal) {
        String normUser = normalizer.normalizeName(userVal);
        String normOcr  = normalizer.normalizeName(ocrVal);

        boolean userIsArabic = transliterator.containsArabic(normUser);
        boolean ocrIsArabic  = transliterator.containsArabic(normOcr);

        int score;

        if (!userIsArabic && !ocrIsArabic) {
            // Both Latin — straight similarity
            score = levenshteinSimilarity(normUser, normOcr);

        } else if (userIsArabic && ocrIsArabic) {
            // Both Arabic — straight similarity on normalized Arabic
            score = levenshteinSimilarity(normUser, normOcr);

        } else {
            // Mixed script — transliterate both ways and take best score
            String arabicSide = userIsArabic ? normUser : normOcr;
            String latinSide  = userIsArabic ? normOcr  : normUser;

            // Path A: Arabic side → Latin, compare with Latin side
            String arabicAsLatin = transliterator.arabicToLatin(arabicSide);
            int scoreA = levenshteinSimilarity(arabicAsLatin, latinSide);

            // Path B: Latin side → Arabic, compare with Arabic side
            String latinAsArabic = transliterator.latinToArabic(latinSide);
            // Also normalize the reverse-transliterated result
            String normLatinAsArabic = normalizer.normalizeArabicLetters(
                    normalizer.removeArabicDiacritics(latinAsArabic));
            int scoreB = levenshteinSimilarity(normLatinAsArabic, arabicSide);

            score = Math.max(scoreA, scoreB);

            log.debug("Cross-script name comparison — arabicToLatin={}, scoreA={}, scoreB={}",
                    arabicAsLatin, scoreA, scoreB);
        }

        String status  = statusFromScore(score, false);
        String message = buildNameMessage(status, field, userVal, ocrVal, score);

        return FieldComparisonResult.builder()
                .fieldKey(field)
                .fieldLabel(LABELS.getOrDefault(field, field))
                .userValue(userVal)
                .ocrValue(ocrVal)
                .status(status)
                .score(score)
                .message(message)
                .strategy(STRATEGY_FUZZY_NAME)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy: DATE
    // Both sides normalized to YYYY-MM-DD then exact comparison.
    // ─────────────────────────────────────────────────────────────────────────

    private FieldComparisonResult compareDate(String field, String userVal, String ocrVal) {
        String normUser = normalizer.normalizeDate(userVal);
        String normOcr  = normalizer.normalizeDate(ocrVal);

        boolean match = normUser.equals(normOcr);
        int score = match ? 100 : 0;
        String status = match ? MATCH : MISMATCH;
        String message = match ? null
                : "Birth date does not match after format normalization. "
                  + "User entered: " + userVal + " — OCR extracted: " + ocrVal
                  + ". An agent must verify the original document.";

        return FieldComparisonResult.builder()
                .fieldKey(field)
                .fieldLabel(LABELS.getOrDefault(field, field))
                .userValue(userVal)
                .ocrValue(ocrVal)
                .status(status)
                .score(score)
                .message(message)
                .strategy(STRATEGY_DATE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy: ADDRESS
    // Token-based overlap on normalized address strings.
    // Jaccard similarity on word sets after normalization.
    // ─────────────────────────────────────────────────────────────────────────

    private FieldComparisonResult compareAddress(String field, String userVal, String ocrVal) {
        String normUser = normalizer.normalizeAddress(userVal);
        String normOcr  = normalizer.normalizeAddress(ocrVal);

        Set<String> userTokens = tokenize(normUser);
        Set<String> ocrTokens  = tokenize(normOcr);

        int score = jaccardSimilarity(userTokens, ocrTokens);
        String status  = statusFromScore(score, false);
        String message = buildAddressMessage(status, score);

        return FieldComparisonResult.builder()
                .fieldKey(field)
                .fieldLabel(LABELS.getOrDefault(field, field))
                .userValue(userVal)
                .ocrValue(ocrVal)
                .status(status)
                .score(score)
                .message(message)
                .strategy(STRATEGY_ADDRESS)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Report assembly
    // ─────────────────────────────────────────────────────────────────────────

    private OcrComparisonReport buildReport(
            String docType,
            Map<String, FieldComparisonResult> results,
            List<String> skipped) {

        if (results.isEmpty()) {
            return OcrComparisonReport.builder()
                    .overallStatus(MANUAL_REVIEW)
                    .overallScore(0)
                    .documentType(docType)
                    .summaryMessage("No fields could be compared — both user input and OCR data are required.")
                    .requiresAgentReview(true)
                    .fields(results)
                    .skippedFields(skipped)
                    .build();
        }

        // Weighted average: document number & date → weight 2, others → weight 1
        long   totalWeight = 0;
        long   weightedSum = 0;
        String worstStatus = MATCH;
        boolean hasMismatchOnCritical = false;

        for (FieldComparisonResult r : results.values()) {
            int weight = isCriticalField(r.getFieldKey()) ? 2 : 1;
            totalWeight += weight;
            weightedSum += (long) r.getScore() * weight;

            worstStatus = worseStatus(worstStatus, r.getStatus());
            if (isCriticalField(r.getFieldKey()) && MISMATCH.equals(r.getStatus())) {
                hasMismatchOnCritical = true;
            }
        }

        // Critical field mismatch always overrides overall status
        String overallStatus = hasMismatchOnCritical ? MISMATCH : worstStatus;
        int overallScore = (int) (weightedSum / totalWeight);

        boolean requiresReview = !MATCH.equals(overallStatus);
        String summary = buildSummaryMessage(overallStatus, overallScore, results.size(), skipped.size());

        return OcrComparisonReport.builder()
                .overallStatus(overallStatus)
                .overallScore(overallScore)
                .documentType(docType)
                .summaryMessage(summary)
                .requiresAgentReview(requiresReview)
                .fields(results)
                .skippedFields(skipped)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Normalized Levenshtein similarity: 100 × (1 - distance / maxLen).
     * Returns 100 for identical strings, 0 for completely different strings.
     */
    private int levenshteinSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;

        int distance = LEVENSHTEIN.apply(a, b);
        int maxLen   = Math.max(a.length(), b.length());
        return (int) Math.round(100.0 * (1.0 - (double) distance / maxLen));
    }

    /**
     * Jaccard similarity on word token sets: |intersection| / |union| × 100.
     */
    private int jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (int) Math.round(100.0 * intersection.size() / union.size());
    }

    /** Split a normalized string into a set of word tokens (min 2 chars). */
    private Set<String> tokenize(String s) {
        Set<String> tokens = new HashSet<>();
        for (String token : s.split("\\s+")) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    /**
     * Map an integer score to a status string.
     *
     * @param strict when true, only MATCH and MISMATCH are returned (for exact fields)
     */
    private String statusFromScore(int score, boolean strict) {
        if (strict) return score == 100 ? MATCH : MISMATCH;
        if (score >= 95) return MATCH;
        if (score >= 70) return POSSIBLE_MATCH;
        if (score >= 50) return MANUAL_REVIEW;
        return MISMATCH;
    }

    /** Returns the worse of two status values per the severity ordering. */
    private String worseStatus(String a, String b) {
        List<String> order = List.of(MATCH, POSSIBLE_MATCH, MANUAL_REVIEW, MISMATCH);
        return order.indexOf(a) >= order.indexOf(b) ? a : b;
    }

    /** Document number and date fields are "critical" — mismatches escalate overall status. */
    private boolean isCriticalField(String field) {
        return field.equals("cinNumber") || field.equals("passportNumber") || field.equals("birthDate");
    }

    private String buildNameMessage(String status, String field, String userVal, String ocrVal, int score) {
        return switch (status) {
            case MATCH -> null;
            case POSSIBLE_MATCH ->
                    "Name spelling variants detected (score " + score + "/100). "
                    + "This may be a different romanization of the same name. "
                    + "Agent review recommended to confirm identity.";
            case MANUAL_REVIEW ->
                    "Significant name difference detected (score " + score + "/100). "
                    + "OCR quality may be poor, or the names may differ. "
                    + "Mandatory agent review required before proceeding.";
            default ->
                    "Names do not match after Arabic/Latin normalization (score " + score + "/100). "
                    + "An authorized DGI agent must verify the original document.";
        };
    }

    private String buildAddressMessage(String status, int score) {
        return switch (status) {
            case MATCH -> null;
            case POSSIBLE_MATCH ->
                    "Address partially matches (score " + score + "/100). "
                    + "Minor formatting differences detected.";
            case MANUAL_REVIEW ->
                    "Address match is ambiguous (score " + score + "/100). "
                    + "Agent should verify the address against the original document.";
            default ->
                    "Addresses do not match (score " + score + "/100). "
                    + "An agent must verify the original document.";
        };
    }

    private String buildSummaryMessage(String overall, int score, int compared, int skipped) {
        String base = switch (overall) {
            case MATCH -> "All " + compared + " compared field(s) matched successfully (score: " + score + "/100).";
            case POSSIBLE_MATCH ->
                    "Most fields matched but name spelling variants were detected (score: " + score + "/100). "
                    + "Agent review is recommended.";
            case MANUAL_REVIEW ->
                    "One or more fields could not be confidently verified (score: " + score + "/100). "
                    + "Mandatory agent review before approving this application.";
            default ->
                    "One or more critical fields did not match (score: " + score + "/100). "
                    + "An authorized DGI agent must verify the original document before proceeding.";
        };
        if (skipped > 0) {
            base += " (" + skipped + " field(s) were skipped due to missing data.)";
        }
        return base;
    }
}
