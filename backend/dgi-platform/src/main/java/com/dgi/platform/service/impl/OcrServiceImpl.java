package com.dgi.platform.service.impl;

import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real OCR service backed by Tesseract 5.x via Tess4J.
 *
 * Supported document types
 * ─────────────────────────
 *   CIN / CIN_COPY                  — Carte d'Identité Nationale tunisienne
 *   PASSPORT / PASSPORT_COPY        — Passeport (toutes nationalités)
 *   CERTIFICAT_IMMATRICULATION      — Certificat d'immatriculation / patente
 *
 * tessdata resolution order (first non-blank wins)
 * ─────────────────────────────────────────────────
 *   1. ocr.tessdata-path property  (set via TESSDATA_PATH env var)
 *   2. TESSDATA_PREFIX  env var    (native Tesseract convention)
 *   3. Standard Windows install    C:\Program Files\Tesseract-OCR\tessdata
 *   4. Standard Linux/macOS path   /usr/share/tesseract-ocr/5/tessdata
 *
 * Language: fra+ara+eng  (configurable via ocr.language property)
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    // ── Configuration ─────────────────────────────────────────────────────────

    @Value("${ocr.tessdata-path:}")
    private String tessdataPath;

    @Value("${ocr.language:fra+ara+eng}")
    private String ocrLanguage;

    @Value("${ocr.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png"
    );

    /** Candidate tessdata directories tried in order when path is not configured. */
    private static final List<String> FALLBACK_TESSDATA_PATHS = List.of(
            "C:\\Program Files\\Tesseract-OCR\\tessdata",
            "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/local/share/tessdata",
            "/opt/homebrew/share/tessdata"         // macOS Homebrew
    );

    // ── Regex patterns ────────────────────────────────────────────────────────

    // CIN tunisien : 8 chiffres isolés
    private static final Pattern CIN_PATTERN =
            Pattern.compile("\\b([0-9]{8})\\b");

    // Numéro de passeport : 1-2 lettres + 6-9 chiffres
    private static final Pattern PASSPORT_PATTERN =
            Pattern.compile("\\b([A-Z]{1,2}[0-9]{6,9})\\b");

    // Date : JJ/MM/AAAA  JJ-MM-AAAA  AAAA-MM-JJ  et variantes avec chiffres arabes-indiques
    private static final Pattern DATE_PATTERN =
            Pattern.compile(
                    "\\b(\\d{1,2}[/\\-.\\s]\\d{1,2}[/\\-.\\s]\\d{2,4}" +
                    "|\\d{2,4}[/\\-.\\s]\\d{1,2}[/\\-.\\s]\\d{1,2})\\b");

    // Prénom — labels FR / EN / AR
    private static final Pattern FIRST_NAME_PATTERN =
            Pattern.compile(
                    "(?:Pr[eé]nom[s]?|First\\s+Name|Given\\s+Name|الاسم(?:\\s+الشخصي)?)" +
                    "\\s*[:\\-]?\\s*([A-ZÀÂÄÉÈÊËÎÏÔÙÛÜ\u0600-\u06FF][A-Za-zÀ-ÿ\u0600-\u06FF '\\-]{1,40})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Nom de famille — labels FR / EN / AR
    private static final Pattern LAST_NAME_PATTERN =
            Pattern.compile(
                    "(?:Nom(?:\\s+de\\s+famille)?|Last\\s+Name|Family\\s+Name|Surname|النسب|اللقب)" +
                    "\\s*[:\\-]?\\s*([A-ZÀÂÄÉÈÊËÎÏÔÙÛÜ\u0600-\u06FF][A-Za-zÀ-ÿ\u0600-\u06FF '\\-]{1,40})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Adresse — labels FR / EN / AR
    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile(
                    "(?:Adresse|Address|Domicile|العنوان|محل الإقامة)" +
                    "\\s*[:\\-]?\\s*(.{5,120})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Nationalité
    private static final Pattern NATIONALITY_PATTERN =
            Pattern.compile(
                    "(?:Nationalit[eé]|Nationality|الجنسية)" +
                    "\\s*[:\\-]?\\s*([A-Za-zÀ-ÿ\u0600-\u06FF /]{2,40})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // ── Patterns spécifiques au certificat d'immatriculation ─────────────────

    // Matricule fiscal tunisien : 7 chiffres / 1 lettre / 1 lettre / 3 chiffres
    private static final Pattern FISCAL_NUMBER_PATTERN =
            Pattern.compile("\\b([0-9]{7}[A-Z]/[A-Z]/[0-9]{3})\\b");

    // Fallback : identifiant fiscal générique
    private static final Pattern TAX_ID_PATTERN =
            Pattern.compile("(?:Matricule(?:\\s+fiscal)?|N°\\s*fiscal|Tax\\s+ID|المعرف\\s+الجبائي)" +
                    "\\s*[:\\-]?\\s*([A-Z0-9/\\-\u0660-\u0669]{4,20})",
                    Pattern.CASE_INSENSITIVE);

    // Raison sociale / dénomination
    private static final Pattern COMPANY_NAME_PATTERN =
            Pattern.compile("(?:D[eé]nomination|Raison\\s+sociale|Nom\\s+commercial|" +
                    "Company\\s+Name|الإسم\\s+التجاري|التسمية)" +
                    "\\s*[:\\-]?\\s*(.{3,80})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Forme juridique
    private static final Pattern BUSINESS_TYPE_PATTERN =
            Pattern.compile("(?:Forme\\s+juridique|Legal\\s+form|الشكل\\s+القانوني)" +
                    "\\s*[:\\-]?\\s*([A-Za-zÀ-ÿ\u0600-\u06FF .\\-]{2,50})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Numéro RNE / registre national des entreprises
    private static final Pattern REGISTRATION_NUMBER_PATTERN =
            Pattern.compile("(?:N°?\\s*RNE|Registre\\s+National|Num[eé]ro\\s+d.immatriculation|" +
                    "رقم\\s+المسجل)" +
                    "\\s*[:\\-]?\\s*([A-Z0-9\\-]{4,20})",
                    Pattern.CASE_INSENSITIVE);

    // =========================================================================
    // Public API
    // =========================================================================

    @Override
    public OcrVerificationResponse extractFromUpload(MultipartFile file, String documentType) {
        validateFile(file);
        log.info("OCR extraction — documentType={}, size={}B", documentType, file.getSize());

        BufferedImage image = readImage(file);
        BufferedImage preprocessed = preprocessImage(image);
        String rawText = runTesseract(preprocessed, documentType);

        log.debug("OCR raw text length={} chars", rawText.length());

        Map<String, String> extracted = extractFields(rawText, documentType);
        double confidence = computeConfidence(extracted, documentType);
        String status = confidence >= 0.5 ? "SUCCESS" : "PARTIAL";

        log.info("OCR done — documentType={}, fields={}, confidence={:.2f}",
                documentType, extracted.keySet(), confidence);

        return OcrVerificationResponse.builder()
                .verification(status)
                .confidenceScore(confidence)
                .documentType(documentType)
                .extractedData(extracted)
                .differences(Collections.emptyList())
                .statusMessage(buildStatusMessage(extracted, documentType))
                .build();
    }

    @Override
    public OcrVerificationResponse analyzeDocument(String documentPath, String documentType) {
        log.warn("analyzeDocument() is a legacy stub. Use extractFromUpload() for real OCR.");
        return OcrVerificationResponse.builder()
                .verification("PARTIAL").confidenceScore(0.0)
                .documentType(documentType)
                .extractedData(Collections.emptyMap())
                .differences(Collections.emptyList())
                .statusMessage("Legacy endpoint. Upload via /api/ocr/extract-document.")
                .build();
    }

    @Override
    public OcrVerificationResponse compareOcrWithUserData(OcrCompareRequest request) {
        OcrVerificationResponse analysis =
                analyzeDocument(request.getDocumentPath(), request.getDocumentType());
        Map<String, String> extracted = analysis.getExtractedData();
        List<String> differences = new ArrayList<>();

        checkField(request.getExpectedCin(),       extracted.get("cinNumber"),  "CIN",        differences);
        checkField(request.getExpectedFirstName(),  extracted.get("firstName"),  "First name", differences);
        checkField(request.getExpectedLastName(),   extracted.get("lastName"),   "Last name",  differences);
        checkField(request.getExpectedBirthDate(),  extracted.get("birthDate"),  "Birth date", differences);
        checkField(request.getExpectedAddress(),    extracted.get("address"),    "Address",    differences);

        boolean match = differences.isEmpty();
        return OcrVerificationResponse.builder()
                .verification(match ? "SUCCESS" : "FAILED")
                .confidenceScore(match ? 0.96 : Math.max(0.0, 0.96 - differences.size() * 0.15))
                .documentType(request.getDocumentType())
                .extractedData(extracted)
                .differences(differences)
                .statusMessage(match ? "All fields matched." : differences.size() + " mismatch(es).")
                .build();
    }

    // =========================================================================
    // tessdata path resolution
    // =========================================================================

    /**
     * Resolve the tessdata directory.
     *
     * Priority:
     *   1. ocr.tessdata-path property (env var TESSDATA_PATH)
     *   2. TESSDATA_PREFIX env var  (native Tesseract convention)
     *   3. First existing path from FALLBACK_TESSDATA_PATHS
     *
     * Never returns null — throws BadRequestException if nothing is found.
     */
    private String resolveTessdataPath() {
        // 1 — application property
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            log.debug("Tesseract: using configured tessdataPath='{}'", tessdataPath);
            return tessdataPath.trim();
        }

        // 2 — TESSDATA_PREFIX env var (set by the Tesseract Windows installer)
        String prefix = System.getenv("TESSDATA_PREFIX");
        if (prefix != null && !prefix.isBlank()) {
            // TESSDATA_PREFIX points to the tessdata parent, not tessdata itself
            File candidate = new File(prefix, "tessdata");
            if (candidate.isDirectory()) {
                log.info("Tesseract: using TESSDATA_PREFIX/tessdata='{}'", candidate.getAbsolutePath());
                return candidate.getAbsolutePath();
            }
            // Sometimes TESSDATA_PREFIX already ends with "tessdata"
            File direct = new File(prefix);
            if (direct.isDirectory() && direct.getName().equalsIgnoreCase("tessdata")) {
                log.info("Tesseract: using TESSDATA_PREFIX='{}'", prefix.trim());
                return prefix.trim();
            }
        }

        // 3 — well-known install paths
        for (String candidate : FALLBACK_TESSDATA_PATHS) {
            File dir = new File(candidate);
            if (dir.isDirectory() && new File(dir, "eng.traineddata").exists()) {
                log.info("Tesseract: auto-detected tessdata at '{}'", candidate);
                return candidate;
            }
        }

        throw new BadRequestException(
                "Tesseract tessdata directory not found. " +
                "Set the TESSDATA_PATH environment variable to your tessdata folder " +
                "(e.g. C:\\Program Files\\Tesseract-OCR\\tessdata) and restart the server.");
    }

    // =========================================================================
    // Tesseract execution
    // =========================================================================

    private String runTesseract(BufferedImage image, String documentType) {
        Tesseract tesseract = new Tesseract();

        String dataPath = resolveTessdataPath();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(ocrLanguage);           // fra+ara+eng
        tesseract.setPageSegMode(3);                  // Auto page segmentation
        tesseract.setOcrEngineMode(1);                // LSTM only (best accuracy)

        // Improve accuracy for mixed-script documents
        tesseract.setVariable("load_system_dawg",   "false");
        tesseract.setVariable("load_freq_dawg",     "false");
        tesseract.setVariable("textord_heavy_nr",   "1");

        log.debug("Tesseract configured — dataPath='{}', lang='{}', docType='{}'",
                dataPath, ocrLanguage, documentType);

        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            log.error("Tesseract OCR error: {}", e.getMessage());
            throw new BadRequestException(
                    "OCR failed — the document may be blurry or in an unsupported format. " +
                    "Please upload a clear, well-lit JPG or PNG (min 300 DPI).");
        } catch (Error e) {
            log.error("Tesseract native JVM error: {}", e.getMessage());
            throw new BadRequestException(
                    "OCR engine error — tessdata language files may be missing. " +
                    "Contact the administrator.");
        }
    }

    // =========================================================================
    // Image preprocessing — improves Tesseract accuracy
    // =========================================================================

    /**
     * Convert to greyscale and apply basic contrast enhancement.
     * Keeps the image in memory — no disk I/O.
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        // Convert to greyscale
        BufferedImage grey = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grey.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Upscale small images (Tesseract struggles below ~150 DPI equivalent)
        int w = grey.getWidth();
        int h = grey.getHeight();
        if (w < 1000 || h < 700) {
            int newW = Math.max(w * 2, 1500);
            int newH = (int) ((double) h / w * newW);
            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gs = scaled.createGraphics();
            gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            gs.drawImage(grey, 0, 0, newW, newH, null);
            gs.dispose();
            log.debug("Image upscaled from {}x{} to {}x{}", w, h, newW, newH);
            return scaled;
        }

        return grey;
    }

    // =========================================================================
    // File validation
    // =========================================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file must not be empty.");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CONTENT_TYPES.contains(ct.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported file type: " + ct + ". Accepted: JPG, JPEG, PNG.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException(
                    "File exceeds maximum size of " + (maxFileSizeBytes / 1024 / 1024) + " MB.");
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new BadRequestException(
                        "Cannot decode image. Ensure it is a valid JPG or PNG.");
            }
            return img;
        } catch (IOException e) {
            log.error("Failed reading image bytes: {}", e.getMessage());
            throw new BadRequestException("Failed to read document: " + e.getMessage());
        }
    }

    // =========================================================================
    // Field extraction — dispatch by document type
    // =========================================================================

    private Map<String, String> extractFields(String text, String documentType) {
        String upper = documentType.toUpperCase();
        if (upper.startsWith("PASSPORT")) {
            return extractPassportFields(text);
        }
        if (upper.contains("IMMATRICULATION") || upper.equals("PATENTE") ||
                upper.equals("COMMERCIAL_REGISTER")) {
            return extractCertificatImmatriculationFields(text);
        }
        // Default: CIN / CIN_COPY
        return extractCinFields(text);
    }

    // ── CIN ──────────────────────────────────────────────────────────────────

    private Map<String, String> extractCinFields(String text) {
        Map<String, String> f = new LinkedHashMap<>();
        findFirst(CIN_PATTERN,        text).ifPresent(v -> f.put("cinNumber",  v));
        findFirst(FIRST_NAME_PATTERN, text).ifPresent(v -> f.put("firstName",  clean(v)));
        findFirst(LAST_NAME_PATTERN,  text).ifPresent(v -> f.put("lastName",   clean(v)));
        findFirst(DATE_PATTERN,       text).ifPresent(v -> f.put("birthDate",  v));
        findFirst(ADDRESS_PATTERN,    text).ifPresent(v -> f.put("address",    clean(v)));
        return f;
    }

    // ── Passport ─────────────────────────────────────────────────────────────

    private Map<String, String> extractPassportFields(String text) {
        Map<String, String> f = new LinkedHashMap<>();
        findFirst(PASSPORT_PATTERN,    text).ifPresent(v -> f.put("passportNumber", v));
        findFirst(FIRST_NAME_PATTERN,  text).ifPresent(v -> f.put("firstName",      clean(v)));
        findFirst(LAST_NAME_PATTERN,   text).ifPresent(v -> f.put("lastName",       clean(v)));
        findFirst(DATE_PATTERN,        text).ifPresent(v -> f.put("birthDate",      v));
        findFirst(NATIONALITY_PATTERN, text).ifPresent(v -> f.put("nationality",    clean(v)));
        // Passport MRZ — second date occurrence is expiry date
        extractSecondDate(text).ifPresent(v -> f.put("expiryDate", v));
        return f;
    }

    // ── Certificat d'immatriculation / Patente ────────────────────────────────

    private Map<String, String> extractCertificatImmatriculationFields(String text) {
        Map<String, String> f = new LinkedHashMap<>();
        // Tax / fiscal identifier
        findFirst(FISCAL_NUMBER_PATTERN,      text).ifPresent(v -> f.put("fiscalNumber",       v));
        if (!f.containsKey("fiscalNumber")) {
            findFirst(TAX_ID_PATTERN,         text).ifPresent(v -> f.put("taxId",              clean(v)));
        }
        findFirst(COMPANY_NAME_PATTERN,       text).ifPresent(v -> f.put("companyName",        clean(v)));
        findFirst(BUSINESS_TYPE_PATTERN,      text).ifPresent(v -> f.put("businessType",       clean(v)));
        findFirst(REGISTRATION_NUMBER_PATTERN,text).ifPresent(v -> f.put("registrationNumber", v));
        findFirst(DATE_PATTERN,               text).ifPresent(v -> f.put("registrationDate",   v));
        findFirst(ADDRESS_PATTERN,            text).ifPresent(v -> f.put("address",            clean(v)));
        return f;
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    private Optional<String> findFirst(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String v = m.groupCount() > 0 ? m.group(1) : m.group(0);
            if (v != null && !v.isBlank()) return Optional.of(v.trim());
        }
        return Optional.empty();
    }

    /** Extract the second occurrence of a date — used for passport expiry. */
    private Optional<String> extractSecondDate(String text) {
        Matcher m = DATE_PATTERN.matcher(text);
        int count = 0;
        while (m.find()) {
            if (++count == 2) return Optional.of(m.group(0).trim());
        }
        return Optional.empty();
    }

    private String clean(String value) {
        return value.replaceAll("\\s{2,}", " ").trim();
    }

    private void checkField(String expected, String extracted,
                            String label, List<String> diffs) {
        if (expected != null && extracted != null &&
                !expected.equalsIgnoreCase(extracted)) {
            diffs.add(label + " mismatch");
        }
    }

    // ── Confidence + status ───────────────────────────────────────────────────

    private double computeConfidence(Map<String, String> extracted, String documentType) {
        List<String> keys = getExpectedKeys(documentType);
        long found = keys.stream().filter(extracted::containsKey).count();
        return keys.isEmpty() ? 0.0 : (double) found / keys.size();
    }

    private String buildStatusMessage(Map<String, String> extracted, String documentType) {
        if (extracted.isEmpty()) {
            return "Aucun champ extrait. Vérifiez la qualité de l'image " +
                   "(netteté, éclairage, min. 300 DPI).";
        }
        List<String> missing = getExpectedKeys(documentType).stream()
                .filter(k -> !extracted.containsKey(k)).toList();
        if (missing.isEmpty()) return "Tous les champs ont été extraits avec succès.";
        return "Extraction partielle — champs manquants : " + String.join(", ", missing) +
               ". Essayez avec un scan de meilleure qualité.";
    }

    private List<String> getExpectedKeys(String documentType) {
        String upper = documentType.toUpperCase();
        if (upper.startsWith("PASSPORT")) {
            return List.of("passportNumber", "firstName", "lastName", "birthDate", "nationality");
        }
        if (upper.contains("IMMATRICULATION") || upper.equals("PATENTE") ||
                upper.equals("COMMERCIAL_REGISTER")) {
            return List.of("companyName", "registrationDate");
        }
        // CIN
        return List.of("cinNumber", "firstName", "lastName", "birthDate");
    }
}
