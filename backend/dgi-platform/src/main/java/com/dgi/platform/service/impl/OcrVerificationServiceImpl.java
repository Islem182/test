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

import java.util.*;

/**
 * Verification engine for online fiscal registration (immatriculation fiscale).
 *
 * Steps & Documents:
 * 1. CIN:
 *    - Numéro CIN (8 chiffres)
 *    - Nom
 *    - Prénom
 *    -> Compared with Step 1 user inputs.
 *
 * 2. Passport:
 *    - Numéro de passeport
 *    - Nom
 *    - Prénom
 *    -> Passport number compared with Step 1.
 *    -> Passport Nom/Prénom cross-checked for compatibility with CIN & Step 1 identity.
 *
 * 3. Certificat d'immatriculation au registre du commerce:
 *    - Date de début d'activité (startDate / registrationDate)
 *    - Dénomination sociale (businessName / companyName)
 *    - Forme juridique (businessType)
 *    - Siège social (businessAddress / address)
 *    - Secteur d'activité (sector)
 *    -> Compared with Step 2 commercial activity inputs.
 *
 * Status returned for every field:
 *   MATCH     — document value matches user input
 *   MISMATCH  — document value differs from user input
 *   NOT_FOUND — OCR could not find the field
 *
 * Final Result:
 *   ✅ Documents vérifiés / informations cohérentes
 *   ❌ Échec de la vérification / informations incohérentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrVerificationServiceImpl implements OcrVerificationService {

    private final TextNormalizationService normalizer;
    private final ArabicLatinTransliterator transliterator;

    private static final LevenshteinDistance LEV = LevenshteinDistance.getDefaultInstance();

    // ── Field status constants (Required) ────────────────────────────────────
    public static final String MATCH     = "MATCH";
    public static final String MISMATCH  = "MISMATCH";
    public static final String NOT_FOUND = "NOT_FOUND";

    // ── Document & Report status constants ───────────────────────────────────
    public static final String STATUS_VALID   = "VALID";
    public static final String STATUS_INVALID = "INVALID";

    // ── Verdict messages ─────────────────────────────────────────────────────
    public static final String SUMMARY_VERIFIED = "✅ Documents vérifiés / informations cohérentes";
    public static final String SUMMARY_FAILED   = "❌ Échec de la vérification / informations incohérentes";

    @Override
    public OcrVerificationReport buildReport(OcrVerifyDocumentsRequest request) {
        Map<String, String> cinData      = request.getCinData() != null ? request.getCinData() : Collections.emptyMap();
        Map<String, String> passportData = request.getPassportData() != null ? request.getPassportData() : Collections.emptyMap();
        Map<String, String> certData     = request.getCertificatData() != null ? request.getCertificatData() : Collections.emptyMap();
        Map<String, String> userData     = request.getUserEnteredData() != null ? request.getUserEnteredData() : Collections.emptyMap();

        // ─────────────────────────────────────────────────────────────────────
        // 1. CIN Verification
        // Extract only: Numéro CIN, Nom, Prénom
        // Compare each extracted value with the corresponding value entered in Step 1
        // ─────────────────────────────────────────────────────────────────────
        String userCin       = getField(userData, "cin", "cinNumber", "numeroCin");
        String userLastName  = getField(userData, "lastName", "nom");
        String userFirstName = getField(userData, "firstName", "prenom");

        OcrFieldResult numeroCin = compareDocNumber(
                getField(cinData, "cinNumber", "cin", "numeroCin"),
                userCin,
                "Numéro CIN"
        );

        OcrFieldResult cinNom = compareName(
                getField(cinData, "lastName", "nom"),
                userLastName,
                "Nom"
        );

        OcrFieldResult cinPrenom = compareName(
                getField(cinData, "firstName", "prenom"),
                userFirstName,
                "Prénom"
        );

        boolean cinAllMatch = MATCH.equals(numeroCin.getStatus())
                && MATCH.equals(cinNom.getStatus())
                && MATCH.equals(cinPrenom.getStatus());

        String cinDocStatus = cinAllMatch ? STATUS_VALID : STATUS_INVALID;

        OcrVerificationReport.CinSection cinSection = OcrVerificationReport.CinSection.builder()
                .numeroCin(numeroCin)
                .nom(cinNom)
                .prenom(cinPrenom)
                .documentStatus(cinDocStatus)
                .build();

        // ─────────────────────────────────────────────────────────────────────
        // 2. Passport Verification
        // Extract: Numéro de passeport, Nom, Prénom
        // At minimum, compare passport number with Step 1.
        // Also verify that passport name/prénom are compatible with CIN name/prénom.
        // ─────────────────────────────────────────────────────────────────────
        String userPassport = getField(userData, "passport", "passportNumber", "numeroPassport");

        OcrFieldResult numPassport = compareDocNumber(
                getField(passportData, "passportNumber", "passport", "numeroPassport"),
                userPassport,
                "Numéro de passeport"
        );

        OcrFieldResult passNom = comparePassportName(
                getField(passportData, "lastName", "nom"),
                userLastName,
                getField(cinData, "lastName", "nom"),
                "Nom"
        );

        OcrFieldResult passPrenom = comparePassportName(
                getField(passportData, "firstName", "prenom"),
                userFirstName,
                getField(cinData, "firstName", "prenom"),
                "Prénom"
        );

        // Check compatibility between Passport and CIN / User identity
        CompatResult nomCompat = compareNamesCrossScript(
                getField(cinData, "lastName", "nom"),
                getField(passportData, "lastName", "nom"),
                userLastName
        );
        CompatResult prenomCompat = compareNamesCrossScript(
                getField(cinData, "firstName", "prenom"),
                getField(passportData, "firstName", "prenom"),
                userFirstName
        );
        CompatResult overallCompat = mergeCompat(nomCompat, prenomCompat);

        boolean passAllMatch = MATCH.equals(numPassport.getStatus())
                && MATCH.equals(passNom.getStatus())
                && MATCH.equals(passPrenom.getStatus())
                && !MISMATCH.equals(overallCompat.status);

        String passDocStatus = passAllMatch ? STATUS_VALID : STATUS_INVALID;

        OcrVerificationReport.PassportSection passSection = OcrVerificationReport.PassportSection.builder()
                .numeroPassport(numPassport)
                .nom(passNom)
                .prenom(passPrenom)
                .identityCompatibility(overallCompat.status)
                .compatibilityScore(overallCompat.score)
                .documentStatus(passDocStatus)
                .build();

        // ─────────────────────────────────────────────────────────────────────
        // 3. Certificat d'immatriculation au registre du commerce
        // Extract commercial activity fields entered in Step 2:
        // - Date de début d'activité
        // - Dénomination sociale
        // - Forme juridique
        // - Siège social
        // - Secteur d'activité
        // ─────────────────────────────────────────────────────────────────────
        String userStartDate       = getField(userData, "startDate", "registrationDate", "dateDebutActivite");
        String userBusinessName    = getField(userData, "businessName", "companyName", "denominationSociale");
        String userBusinessType    = getField(userData, "businessType", "formeJuridique");
        String userBusinessAddress = getField(userData, "businessAddress", "address", "siegeSocial");
        String userSector          = getField(userData, "sector", "secteurActivite");

        OcrFieldResult dateDebut = compareDate(
                getField(certData, "registrationDate", "startDate", "dateDebutActivite"),
                userStartDate,
                "Date de début d'activité"
        );

        OcrFieldResult denomination = compareText(
                getField(certData, "companyName", "businessName", "denominationSociale"),
                userBusinessName,
                "Dénomination sociale",
                0.80
        );

        OcrFieldResult forme = compareBusinessType(
                getField(certData, "businessType", "formeJuridique"),
                userBusinessType,
                "Forme juridique"
        );

        OcrFieldResult siege = compareAddress(
                getField(certData, "address", "businessAddress", "siegeSocial"),
                userBusinessAddress,
                "Siège social"
        );

        OcrFieldResult secteur = compareSector(
                getField(certData, "sector", "secteurActivite"),
                userSector,
                "Secteur d'activité"
        );

        boolean certAllMatch = MATCH.equals(dateDebut.getStatus())
                && MATCH.equals(denomination.getStatus())
                && MATCH.equals(forme.getStatus())
                && MATCH.equals(siege.getStatus())
                && MATCH.equals(secteur.getStatus());

        String certDocStatus = certAllMatch ? STATUS_VALID : STATUS_INVALID;

        OcrVerificationReport.CertificatSection certSection = OcrVerificationReport.CertificatSection.builder()
                .dateDebutActivite(dateDebut)
                .denominationSociale(denomination)
                .formeJuridique(forme)
                .siegeSocial(siege)
                .secteurActivite(secteur)
                .documentStatus(certDocStatus)
                .build();

        // ─────────────────────────────────────────────────────────────────────
        // 4. Overall Evaluation & Final Result Banner
        // ─────────────────────────────────────────────────────────────────────
        boolean allVerified = cinAllMatch && passAllMatch && certAllMatch;
        String overallStatus = allVerified ? STATUS_VALID : STATUS_INVALID;
        String summary = allVerified ? SUMMARY_VERIFIED : SUMMARY_FAILED;

        log.info("OCR verification result: cin={} pass={} cert={} -> overall={}, verified={}",
                cinDocStatus, passDocStatus, certDocStatus, overallStatus, allVerified);

        return OcrVerificationReport.builder()
                .cin(cinSection)
                .passport(passSection)
                .commercialRegistration(certSection)
                .overallStatus(overallStatus)
                .verified(allVerified)
                .identityConsistency(overallCompat.status)
                .summary(summary)
                .build();
    }

    // =========================================================================
    // Comparison Implementations
    // =========================================================================

    /**
     * Compare document number (CIN or Passport).
     * Normalizes Arabic-Indic digits, converts to uppercase, strips whitespace & separators.
     */
    private OcrFieldResult compareDocNumber(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détecté sur le document.")
                    .build();
        }

        String normExtracted = normalizer.normalizeDocumentNumber(extracted);
        String normEntered   = normalizer.normalizeDocumentNumber(entered);

        if (normExtracted.isEmpty()) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " illisible sur le document.")
                    .build();
        }

        if (isBlank(normEntered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted)
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Valeur non renseignée dans le formulaire.")
                    .build();
        }

        boolean matches = normExtracted.equals(normEntered);
        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(matches ? MATCH : MISMATCH)
                .message(matches ? "Concordance exacte." : "Différence détectée avec la valeur saisie.")
                .build();
    }

    /**
     * Compare name (Prénom / Nom).
     * Handles French, Arabic, Latin representations, accents, diacritics, and transliteration.
     */
    private OcrFieldResult compareName(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détecté sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Valeur non renseignée dans le formulaire.")
                    .build();
        }

        String normExtracted = normalizer.normalizeName(extracted);
        String normEntered   = normalizer.normalizeName(entered);

        if (normExtracted.isEmpty()) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " illisible.")
                    .build();
        }

        int score = calculateNameSimilarity(normExtracted, normEntered);
        boolean isMatch = score >= 80 || normExtracted.equalsIgnoreCase(normEntered);

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Concordance confirmée (" + score + "%)." : "Incohérence détectée (" + score + "%).")
                .build();
    }

    /**
     * Compare Passport Name (Nom / Prénom) with user input as reference AND verify consistency with CIN.
     * Supports French, Arabic, English, accents, transliteration and cross-document verification.
     * Returns MATCH, MISMATCH, or NOT_FOUND.
     */
    private OcrFieldResult comparePassportName(
            String passExtracted,
            String userEntered,
            String cinExtracted,
            String label) {
        if (isBlank(passExtracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(userEntered)
                    .status(NOT_FOUND)
                    .message(label + " non détecté sur le passeport.")
                    .build();
        }

        if (isBlank(userEntered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(passExtracted.trim())
                    .enteredValue(userEntered)
                    .status(MISMATCH)
                    .message(label + " non renseigné dans le formulaire.")
                    .build();
        }

        String normPass = normalizer.normalizeName(passExtracted);
        String normUser = normalizer.normalizeName(userEntered);
        String normCin  = isNotBlank(cinExtracted) ? normalizer.normalizeName(cinExtracted) : "";

        // 1. Primary check: compare extracted passport name with user-entered reference value
        int scoreUser = calculateNameSimilarity(normPass, normUser);
        boolean userMatch = scoreUser >= 80 || normPass.equalsIgnoreCase(normUser);

        // 2. Secondary check: verify consistency with CIN (cross-document check)
        boolean cinConsistent = true;
        int scoreCin = 100;
        if (isNotBlank(normCin)) {
            scoreCin = calculateNameSimilarity(normPass, normCin);
            cinConsistent = scoreCin >= 75 || normPass.equalsIgnoreCase(normCin);
        }

        boolean isMatch = userMatch && cinConsistent;
        String msg;
        if (!userMatch) {
            msg = "Incohérence entre le passeport et les informations saisies (" + scoreUser + "%).";
        } else if (!cinConsistent) {
            msg = "Le passeport correspond à la saisie mais diverge du " + label.toLowerCase() + " extrait de la CIN (" + scoreCin + "%).";
        } else {
            msg = "Concordance confirmée avec la saisie (" + scoreUser + "%)" + (isNotBlank(normCin) ? " et cohérent avec la CIN (" + scoreCin + "%)." : ".");
        }

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(passExtracted.trim())
                .enteredValue(userEntered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(msg)
                .build();
    }

    /**
     * Compare Date (e.g. date début d'activité).
     * Normalizes DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD to canonical YYYY-MM-DD.
     */
    private OcrFieldResult compareDate(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détectée sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Date non renseignée dans le formulaire.")
                    .build();
        }

        String normExtracted = normalizer.normalizeDate(extracted);
        String normEntered   = normalizer.normalizeDate(entered);

        boolean isMatch = normExtracted.equals(normEntered);

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Dates identiques." : "Dates non concordantes.")
                .build();
    }

    /**
     * Compare generic text (Dénomination sociale, etc.) with normalized token overlap.
     */
    private OcrFieldResult compareText(String extracted, String entered, String label, double threshold) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détectée sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Valeur non renseignée dans le formulaire.")
                    .build();
        }

        String cleanExtracted = cleanGeneralText(extracted);
        String cleanEntered   = cleanGeneralText(entered);

        if (cleanExtracted.equalsIgnoreCase(cleanEntered) ||
                cleanExtracted.contains(cleanEntered) ||
                cleanEntered.contains(cleanExtracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered.trim())
                    .status(MATCH)
                    .message("Concordance validée.")
                    .build();
        }

        int score = levSimilarity(cleanExtracted, cleanEntered);
        boolean isMatch = (score / 100.0) >= threshold;

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Concordance textuelle (" + score + "%)." : "Différence constatée.")
                .build();
    }

    /**
     * Compare Forme Juridique (SARL, SUARL, SA, etc.).
     */
    private OcrFieldResult compareBusinessType(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détectée sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Valeur non renseignée dans le formulaire.")
                    .build();
        }

        String canExtracted = canonicalBusinessType(extracted);
        String canEntered   = canonicalBusinessType(entered);

        boolean isMatch = canExtracted.equals(canEntered) ||
                cleanGeneralText(extracted).contains(cleanGeneralText(entered)) ||
                cleanGeneralText(entered).contains(cleanGeneralText(extracted));

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Forme juridique concordante." : "Forme juridique différente.")
                .build();
    }

    /**
     * Compare Siège Social (address) with tolerance for city / address tokens.
     */
    private OcrFieldResult compareAddress(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détecté sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Adresse non renseignée dans le formulaire.")
                    .build();
        }

        String normExtracted = normalizer.normalizeAddress(extracted);
        String normEntered   = normalizer.normalizeAddress(entered);

        boolean isMatch = normExtracted.equalsIgnoreCase(normEntered) ||
                normExtracted.contains(normEntered) ||
                normEntered.contains(normExtracted);

        if (!isMatch) {
            // Check token overlap
            Set<String> tokensExtracted = new HashSet<>(Arrays.asList(normExtracted.split("\\s+")));
            Set<String> tokensEntered   = new HashSet<>(Arrays.asList(normEntered.split("\\s+")));
            int common = 0;
            for (String t : tokensEntered) {
                if (t.length() >= 3 && tokensExtracted.contains(t)) common++;
            }
            if (!tokensEntered.isEmpty() && (double) common / tokensEntered.size() >= 0.50) {
                isMatch = true;
            }
        }

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Adresse concordante." : "Adresse non concordante.")
                .build();
    }

    /**
     * Compare Secteur d'activité.
     */
    private OcrFieldResult compareSector(String extracted, String entered, String label) {
        if (isBlank(extracted)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(null)
                    .enteredValue(entered)
                    .status(NOT_FOUND)
                    .message(label + " non détecté sur le document.")
                    .build();
        }

        if (isBlank(entered)) {
            return OcrFieldResult.builder()
                    .fieldName(label)
                    .extractedValue(extracted.trim())
                    .enteredValue(entered)
                    .status(MISMATCH)
                    .message("Secteur non renseigné dans le formulaire.")
                    .build();
        }

        String cleanExt = cleanGeneralText(extracted);
        String cleanEnt = cleanGeneralText(entered);

        boolean isMatch = cleanExt.equalsIgnoreCase(cleanEnt) ||
                cleanExt.contains(cleanEnt) ||
                cleanEnt.contains(cleanExt) ||
                levSimilarity(cleanExt, cleanEnt) >= 70;

        return OcrFieldResult.builder()
                .fieldName(label)
                .extractedValue(extracted.trim())
                .enteredValue(entered.trim())
                .status(isMatch ? MATCH : MISMATCH)
                .message(isMatch ? "Secteur d'activité concordant." : "Secteur d'activité différent.")
                .build();
    }

    // =========================================================================
    // Name comparison & Cross-Document Identity Compatibility
    // =========================================================================

    private record CompatResult(String status, int score) {}

    private CompatResult compareNamesCrossScript(String cinName, String passportName, String userName) {
        String targetPassport = isNotBlank(passportName) ? passportName.trim() : "";
        String targetCin      = isNotBlank(cinName) ? cinName.trim() : "";
        String targetUser     = isNotBlank(userName) ? userName.trim() : "";

        if (isBlank(targetPassport)) {
            return new CompatResult(NOT_FOUND, 0);
        }

        String normPass = normalizer.normalizeName(targetPassport);

        if (isNotBlank(targetCin)) {
            String normCin = normalizer.normalizeName(targetCin);
            int scoreCin = calculateNameSimilarity(normCin, normPass);
            boolean cinMatch = scoreCin >= 75 || normCin.equalsIgnoreCase(normPass);

            if (isNotBlank(targetUser)) {
                String normUser = normalizer.normalizeName(targetUser);
                int scoreUser = calculateNameSimilarity(normUser, normPass);
                boolean userMatch = scoreUser >= 80 || normUser.equalsIgnoreCase(normPass);
                String status = (cinMatch && userMatch) ? MATCH : MISMATCH;
                return new CompatResult(status, Math.min(scoreCin, scoreUser));
            }

            return new CompatResult(cinMatch ? MATCH : MISMATCH, scoreCin);
        }

        if (isNotBlank(targetUser)) {
            String normUser = normalizer.normalizeName(targetUser);
            int scoreUser = calculateNameSimilarity(normUser, normPass);
            return new CompatResult(scoreUser >= 80 ? MATCH : MISMATCH, scoreUser);
        }

        return new CompatResult(NOT_FOUND, 0);
    }

    private int calculateNameSimilarity(String normA, String normB) {
        if (normA.isEmpty() && normB.isEmpty()) return 100;
        if (normA.isEmpty() || normB.isEmpty()) return 0;
        if (normA.equalsIgnoreCase(normB)) return 100;

        // Substring / Token containment: e.g. "BEN SALEM" vs "BEN SALEM MOHAMED"
        if (normA.contains(normB) || normB.contains(normA)) {
            int shorterLen = Math.min(normA.length(), normB.length());
            if (shorterLen >= 3) {
                return 95;
            }
        }

        // Token reordering check: "MOHAMED AMINE" vs "AMINE MOHAMED"
        List<String> tokensA = Arrays.stream(normA.split("\\s+")).filter(s -> !s.isBlank()).toList();
        List<String> tokensB = Arrays.stream(normB.split("\\s+")).filter(s -> !s.isBlank()).toList();
        if (tokensA.size() > 1 && tokensA.size() == tokensB.size() && new HashSet<>(tokensA).equals(new HashSet<>(tokensB))) {
            return 95;
        }

        // Handle mixed-script in either string (e.g. "TRABELSI الطرابلسي")
        boolean aHasArabic = transliterator.containsArabic(normA);
        boolean bHasArabic = transliterator.containsArabic(normB);
        boolean aHasLatin = normA.matches(".*[A-ZÀ-ÿ].*");
        boolean bHasLatin = normB.matches(".*[A-ZÀ-ÿ].*");

        if (aHasArabic && aHasLatin) {
            String aLatin = normA.replaceAll("[^\u0020-\u007EA-ZÀ-ÿ]", " ").replaceAll("\\s+", " ").trim();
            String aArabic = normA.replaceAll("[^\u0600-\u06FF\u0750-\u077F\\s]", " ").replaceAll("\\s+", " ").trim();
            int scoreLatin = aLatin.isEmpty() ? 0 : calculateNameSimilarity(aLatin, normB);
            int scoreArabic = aArabic.isEmpty() ? 0 : calculateNameSimilarity(aArabic, normB);
            return Math.max(scoreLatin, scoreArabic);
        }

        if (bHasArabic && bHasLatin) {
            String bLatin = normB.replaceAll("[^\u0020-\u007EA-ZÀ-ÿ]", " ").replaceAll("\\s+", " ").trim();
            String bArabic = normB.replaceAll("[^\u0600-\u06FF\u0750-\u077F\\s]", " ").replaceAll("\\s+", " ").trim();
            int scoreLatin = bLatin.isEmpty() ? 0 : calculateNameSimilarity(normA, bLatin);
            int scoreArabic = bArabic.isEmpty() ? 0 : calculateNameSimilarity(normA, bArabic);
            return Math.max(scoreLatin, scoreArabic);
        }

        // Check stripped definite article prefix (ال / AL / EL)
        String strippedA = normalizer.stripAlPrefix(normA);
        String strippedB = normalizer.stripAlPrefix(normB);
        int strippedScore = 0;
        if (!strippedA.equals(normA) || !strippedB.equals(normB)) {
            strippedScore = calculateNameSimilarity(strippedA, strippedB);
        }

        if (aHasArabic == bHasArabic) {
            int lev = levSimilarity(normA, normB);
            return Math.max(lev, strippedScore);
        }

        // Cross-script: transliterate Arabic to Latin and compare
        String arabicSide = aHasArabic ? normA : normB;
        String latinSide  = aHasArabic ? normB : normA;

        String transliteratedLatin = transliterator.arabicToLatin(arabicSide);
        int scoreAtoL = levSimilarity(transliteratedLatin, latinSide);

        String reverseArabic = normalizer.normalizeArabicLetters(
                normalizer.removeArabicDiacritics(transliterator.latinToArabic(latinSide))
        );
        int scoreLtoA = levSimilarity(reverseArabic, arabicSide);

        int best = Math.max(scoreAtoL, scoreLtoA);
        return Math.max(best, strippedScore);
    }

    private CompatResult mergeCompat(CompatResult a, CompatResult b) {
        int avgScore = (a.score + b.score) / 2;
        if (MISMATCH.equals(a.status) || MISMATCH.equals(b.status)) {
            return new CompatResult(MISMATCH, avgScore);
        }
        if (NOT_FOUND.equals(a.status) && NOT_FOUND.equals(b.status)) {
            return new CompatResult(NOT_FOUND, 0);
        }
        return new CompatResult(MATCH, avgScore);
    }

    // =========================================================================
    // Normalization & Helper utilities
    // =========================================================================

    private String canonicalBusinessType(String raw) {
        if (raw == null) return "";
        String s = cleanGeneralText(raw).toUpperCase();
        if (s.contains("SUARL") || s.contains("S U A R L") || s.contains("UNIPERSONNELLE")) return "SUARL";
        if (s.contains("SARL") || s.contains("S A R L")) return "SARL";
        if (s.contains("S A") || s.equals("SA") || s.contains("ANONYME")) return "SA";
        if (s.contains("PHYSIQUE") || s.contains("PATENTE") || s.contains("INDIVIDU")) return "PERSONNE_PHYSIQUE";
        return s;
    }

    private String cleanGeneralText(String text) {
        if (text == null) return "";
        String s = normalizer.stripLatinAccents(text.trim());
        s = s.replaceAll("[\\p{Punct}]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private int levSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;
        int dist   = LEV.apply(a.toUpperCase(), b.toUpperCase());
        int maxLen = Math.max(a.length(), b.length());
        return (int) Math.round(100.0 * (1.0 - (double) dist / maxLen));
    }

    private String getField(Map<String, String> map, String... candidateKeys) {
        if (map == null) return null;
        for (String key : candidateKeys) {
            String val = map.get(key);
            if (val != null && !val.isBlank()) return val.trim();
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isNotBlank(String s) {
        return !isBlank(s);
    }
}
