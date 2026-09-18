package com.dgi.platform.service.impl;

import com.dgi.platform.dto.OcrFieldResult;
import com.dgi.platform.dto.OcrVerificationReport;
import com.dgi.platform.dto.OcrVerifyDocumentsRequest;
import com.dgi.platform.service.ArabicLatinTransliterator;
import com.dgi.platform.service.OcrVerificationService;
import com.dgi.platform.service.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Verification engine for Step 4.
 *
 * Per-document rules
 * ──────────────────
 * CIN      → required: cinNumber, firstName, lastName, address
 * Passport → required: passportNumber, firstName, lastName
 *              + cross-check lastName+firstName vs CIN (fuzzy, Arabic/Latin aware)
 * Certificat → required: companyName (denominationSociale), businessType (formeJuridique),
 *                         address (siegeSocial), sector (secteurActivite),
 *                         registrationDate (dateDebutActivite)
 *
 * Normalization stack (delegates to existing services)
 * ─────────────────────────────────────────────────────
 *   - TextNormalizationService  : Arabic diacritics, Arabic-Indic digits, accents,
 *                                  case folding, whitespace collapsing
 *   - ArabicLatinTransliterator : Arabic ↔ Latin phonetic transliteration
 *   - LevenshteinDistance       : fuzzy similarity scoring
 *
 * Identity compatibility thresholds
 * ────────────────────────────────────
 *   ≥ 95  → MATCH
 *   70–94 → POSSIBLE_MATCH
 *   < 70  → MISMATCH
 *   (either value missing) → UNABLE_TO_COMPARE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrVerificationServiceImpl implements OcrVerificationService {

    private final TextNormalizationService   normalizer;
    private final ArabicLatinTransliterator  transliterator;

    private static final LevenshteinDistance LEV = LevenshteinDistance.getDefaultInstance();

    // ── Status constants ───────────────────────────────────────────────────────
    private static final String VALID              = "VALID";
    private static final String MISSING            = "MISSING";
    private static final String INVALID            = "INVALID";
    private static final String MATCH              = "MATCH";
    private static final String POSSIBLE_MATCH     = "POSSIBLE_MATCH";
    private static final String MISMATCH           = "MISMATCH";
    private static final String UNABLE_TO_COMPARE  = "UNABLE_TO_COMPARE";
    private static final String DOC_VALID          = "VALID";
    private static final String DOC_INVALID        = "INVALID";
    private static final String DOC_PARTIAL        = "PARTIAL";

    // =========================================================================
    // Public API
    // =========================================================================

    @Override
    public OcrVerificationReport buildReport(OcrVerifyDocumentsRequest request) {
        Map<String, String> cinData      = request.getCinData();
        Map<String, String> passportData = request.getPassportData();
        Map<String, String> certData     = request.getCertificatData();

        // ── 1. CIN section ────────────────────────────────────────────────────
        OcrFieldResult numeroCin = verifyDocNumber(cinData, "cinNumber",  "Numéro CIN (8 chiffres)");
        OcrFieldResult cinNom    = verifyName     (cinData, "lastName",   "Nom");
        OcrFieldResult cinPrenom = verifyName     (cinData, "firstName",  "Prénom");
        OcrFieldResult cinAdresse= verifyAddress  (cinData, "address",    "Adresse");

        String cinStatus = worstDocStatus(numeroCin, cinNom, cinPrenom, cinAdresse);

        OcrVerificationReport.CinSection cinSection = OcrVerificationReport.CinSection.builder()
                .numeroCin(numeroCin).nom(cinNom).prenom(cinPrenom).adresse(cinAdresse)
                .documentStatus(cinStatus)
                .build();

        // ── 2. Passport section ───────────────────────────────────────────────
        OcrFieldResult numPassport    = verifyDocNumber(passportData, "passportNumber", "Numéro passeport");
        OcrFieldResult passportNom    = verifyName     (passportData, "lastName",       "Nom");
        OcrFieldResult passportPrenom = verifyName     (passportData, "firstName",      "Prénom");

        // Cross-check CIN ↔ Passport identity
        CompatResult nomCompat    = compareName(cinData.get("lastName"),  passportData.get("lastName"));
        CompatResult prenomCompat = compareName(cinData.get("firstName"), passportData.get("firstName"));
        CompatResult overall      = mergeCompat(nomCompat, prenomCompat);

        String passStatus = worstDocStatus(numPassport, passportNom, passportPrenom);

        OcrVerificationReport.PassportSection passSection = OcrVerificationReport.PassportSection.builder()
                .numeroPassport(numPassport).nom(passportNom).prenom(passportPrenom)
                .identityCompatibility(overall.status)
                .compatibilityScore(overall.score)
                .documentStatus(passStatus)
                .build();

        // ── 3. Certificat section ─────────────────────────────────────────────
        OcrFieldResult dateDebut     = verifyGeneric(certData, "registrationDate", "Date de début d'activité");
        OcrFieldResult denomination  = verifyGeneric(certData, "companyName",      "Dénomination sociale");
        OcrFieldResult forme         = verifyGeneric(certData, "businessType",     "Forme juridique");
        OcrFieldResult siege         = verifyAddress(certData, "address",          "Siège social");
        OcrFieldResult secteur       = verifyGeneric(certData, "sector",           "Secteur d'activité");

        // Accept alternative key names the OCR may use
        if (MISSING.equals(secteur.getStatus()))
            secteur = verifyGeneric(certData, "taxId",     "Secteur d'activité");
        if (MISSING.equals(forme.getStatus()))
            forme   = verifyGeneric(certData, "fiscalNumber", "Forme juridique");

        String certStatus = worstDocStatus(dateDebut, denomination, forme, siege, secteur);

        OcrVerificationReport.CertificatSection certSection = OcrVerificationReport.CertificatSection.builder()
                .dateDebutActivite(dateDebut).denominationSociale(denomination)
                .formeJuridique(forme).siegeSocial(siege).secteurActivite(secteur)
                .documentStatus(certStatus)
                .build();

        // ── 4. Overall ────────────────────────────────────────────────────────
        String overallStatus = computeOverall(cinStatus, passStatus, certStatus, overall.status);
        String summary       = buildSummary(overallStatus, overall.status, cinStatus, passStatus, certStatus);

        log.info("OCR verification report — cin={} passport={} cert={} identity={} overall={}",
                cinStatus, passStatus, certStatus, overall.status, overallStatus);

        return OcrVerificationReport.builder()
                .cin(cinSection).passport(passSection).commercialRegistration(certSection)
                .overallStatus(overallStatus)
                .identityConsistency(overall.status)
                .summary(summary)
                .build();
    }

    // =========================================================================
    // Field verifiers
    // =========================================================================

    /** Document number — must be non-blank after normalization. */
    private OcrFieldResult verifyDocNumber(Map<String, String> data, String key, String label) {
        String raw = data.get(key);
        if (raw == null || raw.isBlank())
            return missing(label + " non trouvé dans le document.");
        String norm = normalizer.normalizeDocumentNumber(raw);
        if (norm.isEmpty())
            return fieldResult(null, INVALID, label + " extrait mais illisible.");
        return valid(raw.trim());
    }

    /** Name field — tolerant of Arabic/Latin, diacritics, casing. */
    private OcrFieldResult verifyName(Map<String, String> data, String key, String label) {
        String raw = data.get(key);
        if (raw == null || raw.isBlank())
            return missing(label + " non trouvé dans le document.");
        String norm = normalizer.normalizeName(raw);
        if (norm.isEmpty())
            return fieldResult(null, INVALID, label + " extrait mais illisible.");
        return valid(raw.trim());
    }

    /** Address — non-blank, any content accepted. */
    private OcrFieldResult verifyAddress(Map<String, String> data, String key, String label) {
        String raw = data.get(key);
        if (raw == null || raw.isBlank())
            return missing(label + " non trouvée dans le document.");
        return valid(raw.trim());
    }

    /** Generic text field — just checks presence. */
    private OcrFieldResult verifyGeneric(Map<String, String> data, String key, String label) {
        String raw = data.get(key);
        if (raw == null || raw.isBlank())
            return missing(label + " non trouvé dans le document.");
        return valid(raw.trim());
    }

    // =========================================================================
    // Name comparison — Arabic/Latin aware fuzzy matching
    // =========================================================================

    private record CompatResult(String status, int score) {}

    /**
     * Compare a single name token between CIN and Passport.
     * Handles Arabic ↔ Latin via transliteration; uses Levenshtein similarity.
     */
    private CompatResult compareName(String cinName, String passportName) {
        if (isBlank(cinName) || isBlank(passportName))
            return new CompatResult(UNABLE_TO_COMPARE, 0);

        String normCin  = normalizer.normalizeName(cinName);
        String normPass = normalizer.normalizeName(passportName);

        if (normCin.isEmpty() || normPass.isEmpty())
            return new CompatResult(UNABLE_TO_COMPARE, 0);

        boolean cinArabic  = transliterator.containsArabic(normCin);
        boolean passArabic = transliterator.containsArabic(normPass);

        int score;

        if (cinArabic == passArabic) {
            // Same script — direct Levenshtein
            score = levSimilarity(normCin, normPass);
        } else {
            // Mixed scripts — transliterate both ways, take best
            String arabicSide = cinArabic ? normCin  : normPass;
            String latinSide  = cinArabic ? normPass : normCin;

            int scoreAtoL = levSimilarity(transliterator.arabicToLatin(arabicSide), latinSide);

            String normLatinAsArabic = normalizer.normalizeArabicLetters(
                    normalizer.removeArabicDiacritics(transliterator.latinToArabic(latinSide)));
            int scoreLtoA = levSimilarity(normLatinAsArabic, arabicSide);

            score = Math.max(scoreAtoL, scoreLtoA);
            log.debug("Cross-script name compare: arabicToLatin={} scoreAtoL={} scoreLtoA={}",
                    transliterator.arabicToLatin(arabicSide), scoreAtoL, scoreLtoA);
        }

        String status = score >= 95 ? MATCH
                      : score >= 70 ? POSSIBLE_MATCH
                      : MISMATCH;
        return new CompatResult(status, score);
    }

    /** Merge two name field compatibility results into one (worst wins). */
    private CompatResult mergeCompat(CompatResult a, CompatResult b) {
        if (UNABLE_TO_COMPARE.equals(a.status) && UNABLE_TO_COMPARE.equals(b.status))
            return new CompatResult(UNABLE_TO_COMPARE, 0);
        if (UNABLE_TO_COMPARE.equals(a.status)) return b;
        if (UNABLE_TO_COMPARE.equals(b.status)) return a;

        // Worst status of the two
        String worst = worstCompatStatus(a.status, b.status);
        int avgScore = (a.score + b.score) / 2;
        return new CompatResult(worst, avgScore);
    }

    private String worstCompatStatus(String a, String b) {
        if (MISMATCH.equals(a)       || MISMATCH.equals(b))       return MISMATCH;
        if (POSSIBLE_MATCH.equals(a) || POSSIBLE_MATCH.equals(b)) return POSSIBLE_MATCH;
        return MATCH;
    }

    // =========================================================================
    // Document + overall status helpers
    // =========================================================================

    private String worstDocStatus(OcrFieldResult... fields) {
        boolean anyMissing  = false;
        boolean anyInvalid  = false;
        for (OcrFieldResult f : fields) {
            if (MISSING.equals(f.getStatus()))  anyMissing = true;
            if (INVALID.equals(f.getStatus()))  anyInvalid = true;
        }
        if (anyInvalid)  return DOC_INVALID;
        if (anyMissing)  return DOC_PARTIAL;
        return DOC_VALID;
    }

    private String computeOverall(String cinStatus, String passStatus,
                                   String certStatus, String identityStatus) {
        // Any INVALID document → overall INVALID
        if (DOC_INVALID.equals(cinStatus)  || DOC_INVALID.equals(passStatus) ||
            DOC_INVALID.equals(certStatus) || MISMATCH.equals(identityStatus))
            return DOC_INVALID;

        // All three fully valid + identity match → VALID
        if (DOC_VALID.equals(cinStatus) && DOC_VALID.equals(passStatus) &&
            DOC_VALID.equals(certStatus) &&
            (MATCH.equals(identityStatus) || POSSIBLE_MATCH.equals(identityStatus)))
            return DOC_VALID;

        return DOC_PARTIAL;
    }

    private String buildSummary(String overall, String identity,
                                 String cinStatus, String passStatus, String certStatus) {
        if (DOC_VALID.equals(overall) && MATCH.equals(identity))
            return "Tous les documents sont valides et les identités sont cohérentes.";
        if (DOC_VALID.equals(overall) && POSSIBLE_MATCH.equals(identity))
            return "Documents valides. Des variantes d'orthographe mineures ont été détectées entre la CIN et le passeport — révision agent recommandée.";
        if (MISMATCH.equals(identity))
            return "INCOHÉRENCE DÉTECTÉE : les noms de la CIN et du passeport ne correspondent pas. Révision obligatoire par un agent DGI.";
        if (UNABLE_TO_COMPARE.equals(identity))
            return "Vérification partielle : certains champs n'ont pas pu être extraits. Révision agent requise.";

        StringBuilder sb = new StringBuilder("Vérification incomplète —");
        if (!DOC_VALID.equals(cinStatus))    sb.append(" CIN: ").append(cinStatus).append(";");
        if (!DOC_VALID.equals(passStatus))   sb.append(" Passeport: ").append(passStatus).append(";");
        if (!DOC_VALID.equals(certStatus))   sb.append(" Certificat: ").append(certStatus).append(";");
        sb.append(" Révision agent requise.");
        return sb.toString();
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private int levSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;
        int dist   = LEV.apply(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return (int) Math.round(100.0 * (1.0 - (double) dist / maxLen));
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private OcrFieldResult valid(String value) {
        return OcrFieldResult.builder().value(value).status(VALID).build();
    }

    private OcrFieldResult missing(String message) {
        return OcrFieldResult.builder().value(null).status(MISSING).message(message).build();
    }

    private OcrFieldResult fieldResult(String value, String status, String message) {
        return OcrFieldResult.builder().value(value).status(status).message(message).build();
    }
}
