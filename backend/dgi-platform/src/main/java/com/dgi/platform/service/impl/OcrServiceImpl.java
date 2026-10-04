package com.dgi.platform.service.impl;

import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;
import com.dgi.platform.exception.BadRequestException;
import com.dgi.platform.service.OcrService;
import com.dgi.platform.service.TextNormalizationService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TextNormalizationService normalizer;

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

    // CIN tunisien : 8 chiffres (occidentaux ou arabo-indiques)
    private static final Pattern CIN_STANDALONE_PATTERN =
            Pattern.compile("\\b([0-9\u0660-\u0669]{8})\\b");

    private static final Pattern CIN_LABELED_PATTERN =
            Pattern.compile("(?i)(?:N°|N|CIN|Num[eé]ro|رقم|ب\\.?ت\\.?و|بطاقة\\s*تعريف|Carte)\\s*[:\\-./_\\s]*([0-9\u0660-\u0669]{8})");

    // Numéro de passeport : 1-2 lettres + 6-9 chiffres ou 8-9 chiffres
    private static final Pattern PASSPORT_STANDALONE_PATTERN =
            Pattern.compile("\\b([A-Z]{1,2}[0-9]{6,9}|[0-9]{8,9})\\b");

    private static final Pattern PASSPORT_LABELED_PATTERN =
            Pattern.compile("(?i)(?:Passeport\\s*(?:N°?|No\\.?)|Passport\\s*(?:No\\.?|Number|N°?)|N°\\s*Passeport|رقم\\s*(?:جواز\\s*السفر|الجواز)|Passeport|Passport)\\s*[:\\-./_\\s]*([A-Z0-9\u0660-\u0669]{6,10})");

    // Nom labels regex (French, Arabic, English)
    private static final Pattern NOM_LABEL_REGEX =
            Pattern.compile("(?i)(?:\\bNom(?:\\s+de\\s+famille)?\\b|\\bSurname\\b|\\bLast\\s*Name\\b|\\bFamily\\s*Name\\b|اللقب|النسب)");

    // Prénom labels regex (French, Arabic, English)
    private static final Pattern PRENOM_LABEL_REGEX =
            Pattern.compile("(?i)(?:\\bPr[eé]noms?\\b|\\bGiven\\s*names?\\b|\\bFirst\\s*names?\\b|ال[إا]سم(?:\\s+الشخصي)?|الاسم)");

    // Multiline fallback patterns for CIN & Passport
    private static final Pattern CIN_NOM_MULTILINE =
            Pattern.compile("(?i)(?:اللقب|النسب|Nom(?:\\s+de\\s+famille)?|Surname)(?:\\s*\\([^)]{1,30}\\))?(?:\\s*[/\\\\|\\-—:]\\s*(?:Nom|Surname|اللقب|النسب))*\\s*[:\\-._—|~]*\\s*([A-ZÀÂÄÉÈÊËÎÏÔÙÛÜ\u0600-\u06FF][A-Za-zÀ-ÿ\u0600-\u06FF '\\-]{1,40})");

    private static final Pattern CIN_PRENOM_MULTILINE =
            Pattern.compile("(?i)(?<!اسم\\s|du\\s)(?:Pr[eé]noms?|Given\\s*names?|First\\s*names?|ال[إا]سم(?:\\s+الشخصي)?|الاسم)(?:\\s*\\([^)]{1,30}\\))?(?:\\s*[/\\\\|\\-—:]\\s*(?:Pr[eé]noms?|Given\\s*names?|ال[إا]سم))*\\s*[:\\-._—|~]*\\s*([A-ZÀÂÄÉÈÊËÎÏÔÙÛÜ\u0600-\u06FF][A-Za-zÀ-ÿ\u0600-\u06FF '\\-]{1,40})");

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

    // Secteur d'activité / Industry / Activity
    private static final Pattern SECTOR_PATTERN =
            Pattern.compile("(?:Secteur(?:\\s+d['’]activit[eé])?|Activit[eé](?:\\s+principale)?|Domaine|Industry|Sector|النشاط(?:\\s+الرئيسي)?|قطاع\\s+النشاط)" +
                            "\\s*[:\\-]?\\s*([A-Za-zÀ-ÿ\u0600-\u06FF 0-9/,'\\-]{2,80})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Passport MRZ Line 1: P<ISO_CODE LASTNAME<<FIRSTNAME<<<<<
    private static final Pattern MRZ_LINE_1 =
            Pattern.compile("P[<A-Z]([A-Z]{3})([A-Z<]+)<<([A-Z<]+)");

    // Passport MRZ Line 2: PASSPORT_NUMBER + CHECK_DIGIT + NATIONALITY + DOB...
    private static final Pattern MRZ_LINE_2 =
            Pattern.compile("([A-Z0-9<]{9})[0-9][A-Z<]{3}");

    // =========================================================================
    // Public API
    // =========================================================================

    @Override
    public OcrVerificationResponse extractFromUpload(MultipartFile file, String documentType) {
        validateFile(file);
        log.info("OCR extraction — documentType={}, size={}B", documentType, file.getSize());

        BufferedImage image = readImage(file);

        // Run Tesseract directly on the original image first, exactly matching Tesseract CMD
        String rawText = runTesseract(image, documentType);

        // If rawText is very brief or empty, attempt preprocessed fallback
        if (rawText == null || rawText.trim().length() < 15) {
            log.info("Direct OCR output was brief or empty ({} chars). Trying preprocessed fallback...",
                    rawText == null ? 0 : rawText.trim().length());
            BufferedImage preprocessed = preprocessImage(image);
            String fallbackText = runTesseract(preprocessed, documentType);
            if (fallbackText != null && fallbackText.trim().length() > (rawText == null ? 0 : rawText.trim().length())) {
                rawText = fallbackText;
            }
        }

        // 1. Log raw Tess4J OCR output before any parsing or normalization (strictly required)
        log.info("================================================================================");
        log.info("RAW TESS4J OCR OUTPUT BEFORE ANY PARSING OR NORMALIZATION [documentType={}, filename={}, size={} bytes]:\n{}",
                documentType, file.getOriginalFilename(), file.getSize(), rawText);
        log.info("================================================================================");

        log.debug("OCR raw text length={} chars", rawText != null ? rawText.length() : 0);

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
        tesseract.setPageSegMode(3);                  // Auto page segmentation (default in CMD)
        tesseract.setOcrEngineMode(1);                // LSTM only (default in Tesseract 5.x)

        // Ensure Tess4J matches Tesseract CMD configuration:
        // Do NOT disable system/frequency dictionaries (load_system_dawg / load_freq_dawg remain default true)
        // Do NOT enable textord_heavy_nr which strips genuine character strokes

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
    // Image preprocessing — fallback for low-resolution or low-contrast scans
    // =========================================================================

    /**
     * High-fidelity image scaling fallback for low-resolution documents.
     * Preserves original color depth and prevents text glyph degradation.
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();

        // Upscale small images (Tesseract struggles below ~150-200 DPI equivalent)
        if (w < 1200 || h < 800) {
            int newW = Math.max(w * 2, 1600);
            int newH = (int) ((double) h / w * newW);
            int imageType = original.getType() == 0 ? BufferedImage.TYPE_INT_RGB : original.getType();
            BufferedImage scaled = new BufferedImage(newW, newH, imageType);
            Graphics2D gs = scaled.createGraphics();
            gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gs.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gs.drawImage(original, 0, 0, newW, newH, null);
            gs.dispose();
            log.debug("Image upscaled from {}x{} to {}x{}", w, h, newW, newH);
            return scaled;
        }

        return original;
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
        if (text == null || text.isBlank()) return f;

        // 1. Numéro CIN (8 chiffres)
        extractCinNumber(text).ifPresent(v -> f.put("cinNumber", v));

        // 2. Nom
        extractNameField(text, false).ifPresent(v -> f.put("lastName", v));

        // 3. Prénom
        extractNameField(text, true).ifPresent(v -> f.put("firstName", v));

        log.info("CIN extracted fields: cinNumber={}, lastName={}, firstName={}",
                f.get("cinNumber"), f.get("lastName"), f.get("firstName"));
        return f;
    }

    private Optional<String> extractCinNumber(String text) {
        // 1. Labeled match first (e.g. "N° : 08765432", "CIN : 08765432", "رقم بطاقة التعريف : 08765432")
        Matcher m1 = CIN_LABELED_PATTERN.matcher(text);
        if (m1.find()) {
            String raw = m1.group(1);
            String norm = normalizer.convertArabicIndicToWestern(raw).replaceAll("\\D", "");
            if (norm.length() == 8) return Optional.of(norm);
        }

        // 2. Labeled with OCR noise / letter substitutions e.g. "N° : O8765432"
        Pattern labeledNoisePattern = Pattern.compile(
                "(?i)(?:N°|N|CIN|Num[eé]ro|رقم|ب\\.?ت\\.?و|بطاقة\\s*تعريف|Carte)\\s*[:\\-./_\\s]*([A-Z0-9\u0660-\u0669]{8})"
        );
        Matcher mNoise = labeledNoisePattern.matcher(text);
        if (mNoise.find()) {
            String raw = mNoise.group(1).toUpperCase()
                    .replace('O', '0')
                    .replace('I', '1')
                    .replace('L', '1')
                    .replace('B', '8')
                    .replace('S', '5')
                    .replace('Z', '2');
            String norm = normalizer.convertArabicIndicToWestern(raw).replaceAll("\\D", "");
            if (norm.length() == 8) return Optional.of(norm);
        }

        // 3. Standalone 8 digits fallback
        Matcher m2 = CIN_STANDALONE_PATTERN.matcher(text);
        while (m2.find()) {
            String raw = m2.group(1);
            String norm = normalizer.convertArabicIndicToWestern(raw).replaceAll("\\D", "");
            if (norm.length() == 8) {
                return Optional.of(norm);
            }
        }

        // 4. Digits with spaces e.g. "08 765 432"
        Matcher m3 = Pattern.compile("\\b(\\d{2}\\s+\\d{3}\\s+\\d{3})\\b").matcher(text);
        if (m3.find()) {
            String norm = m3.group(1).replaceAll("\\s+", "");
            if (norm.length() == 8) return Optional.of(norm);
        }

        return Optional.empty();
    }

    // ── Passport ─────────────────────────────────────────────────────────────

    private Map<String, String> extractPassportFields(String text) {
        Map<String, String> f = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return f;

        // 1. Passport Number from VIZ or standalone pattern
        extractPassportNumber(text).ifPresent(v -> f.put("passportNumber", v));

        // 2. Nom and Prénom from Visual Inspection Zone (VIZ)
        extractNameField(text, false).ifPresent(v -> f.put("lastName", v));
        extractNameField(text, true).ifPresent(v -> f.put("firstName", v));

        // 3. MRZ Parser (Machine Readable Zone) — handles noisy outputs and fills missing fields
        extractPassportMrz(text, f);

        log.info("Passport extracted fields: passportNumber={}, lastName={}, firstName={}",
                f.get("passportNumber"), f.get("lastName"), f.get("firstName"));
        return f;
    }

    private Optional<String> extractPassportNumber(String text) {
        Matcher m1 = PASSPORT_LABELED_PATTERN.matcher(text);
        if (m1.find()) {
            String val = m1.group(1).trim();
            val = normalizer.convertArabicIndicToWestern(val).replaceAll("[^A-Z0-9]", "");
            if (val.length() >= 6 && val.length() <= 10) {
                return Optional.of(val);
            }
        }
        Matcher m2 = PASSPORT_STANDALONE_PATTERN.matcher(text);
        while (m2.find()) {
            String val = m2.group(1).trim();
            if (val.length() >= 6 && val.length() <= 10) {
                return Optional.of(val);
            }
        }
        return Optional.empty();
    }

    private void extractPassportMrz(String text, Map<String, String> f) {
        List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        for (String line : lines) {
            String normalizedLine = line
                    .replace('«', '<')
                    .replace('‹', '<')
                    .replace('(', '<')
                    .replace('[', '<')
                    .replace('{', '<')
                    .replaceAll("<\\s+<", "<<")
                    .replaceAll("\\s+", "");

            // MRZ Line 1 candidate (starts with P, contains <<, length >= 25)
            if (normalizedLine.toUpperCase().startsWith("P") && normalizedLine.contains("<<") && normalizedLine.length() >= 25) {
                // Strip P + optional character + 3-letter country code
                String mrzBody = normalizedLine.replaceAll("^(?i)P[<A-Z0-9][A-Z0-9<]{3}", "");
                if (mrzBody.equals(normalizedLine)) {
                    mrzBody = normalizedLine.replaceAll("^(?i)P[<A-Z0-9]{1,4}", "");
                }

                String[] parts = mrzBody.split("<<", 2);
                if (parts.length == 2) {
                    String rawSurname = parts[0].replace('<', ' ').trim();
                    String rawGiven = parts[1].replaceAll("<{2,}.*$", "").replace('<', ' ').trim();

                    String cleanSurname = cleanNameValue(rawSurname);
                    String cleanGiven = cleanNameValue(rawGiven);

                    if (cleanSurname != null && isValidName(cleanSurname)) {
                        if (!f.containsKey("lastName") || f.get("lastName").length() < cleanSurname.length() || !isValidName(f.get("lastName"))) {
                            f.put("lastName", cleanSurname);
                        }
                    }
                    if (cleanGiven != null && isValidName(cleanGiven)) {
                        if (!f.containsKey("firstName") || f.get("firstName").length() < cleanGiven.length() || !isValidName(f.get("firstName"))) {
                            f.put("firstName", cleanGiven);
                        }
                    }
                }
            }

            // MRZ Line 2 candidate (starts with passport number e.g. 8-9 chars followed by digit & 3-letter code)
            if (!f.containsKey("passportNumber") && normalizedLine.length() >= 14) {
                Matcher m2 = Pattern.compile("^([A-Z0-9<]{8,9})[0-9][A-Z<]{3}").matcher(normalizedLine.toUpperCase());
                if (m2.find()) {
                    String pNum = m2.group(1).replace('<', ' ').trim();
                    if (pNum.matches("^[A-Z0-9]{6,10}$")) {
                        f.put("passportNumber", pNum);
                    }
                }
            }
        }
    }

    // ── Generic Name Field Extraction (CIN & Passport) ────────────────────────

    /**
     * Extracts either Nom (lastName) or Prénom (firstName) from document text.
     * Handles:
     * - Labels with parenthetical language descriptors e.g. "Nom (en français)", "Prénom (en français)"
     * - Bilingual labels e.g. "اللقب / Nom : BEN ALI", "الاسم / Prénom : Mohamed"
     * - Values on the same line after separator: "Nom (en français) : BEN ALI"
     * - Values on the next line or lines: "Nom (en français)\nBEN ALI"
     * - Multi-word names: "BEN ALI", "BEN MOHAMED", "MOHAMED ALI"
     * - Skipping non-name intermediate artifacts, language tags, and single characters (e.g. "s")
     * - Never returning a single character
     */
    private Optional<String> extractNameField(String text, boolean isFirstName) {
        if (text == null || text.isBlank()) return Optional.empty();

        List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean isLabelLine = isFirstName ? containsPrenomLabel(line) : containsNomLabel(line);
            if (!isLabelLine) continue;

            // 1. Try extracting on the SAME line after stripping the full label and language qualifiers
            String afterLabel = stripNameLabelPrefix(line, isFirstName);
            String cleanedSameLine = cleanNameValue(afterLabel);
            if (cleanedSameLine != null && isValidName(cleanedSameLine)) {
                return Optional.of(cleanedSameLine);
            }

            // 2. Value is on subsequent lines (inspect up to 4 non-blank lines forward)
            for (int j = i + 1; j < Math.min(i + 5, lines.size()); j++) {
                String candidateLine = lines.get(j);

                // Stop if hitting a label for another distinct field
                if (isOtherFieldLabel(candidateLine, isFirstName)) {
                    break;
                }

                // Skip standalone language descriptor lines e.g. "(en français)", "(بالعربية)"
                if (isLanguageDescriptorLine(candidateLine)) {
                    continue;
                }

                String cleanedCandidate = cleanNameValue(candidateLine);

                // If candidate is a valid name (at least 2 letters, not a stopword or noise letter like 's')
                if (cleanedCandidate != null && isValidName(cleanedCandidate)) {
                    // Check if subsequent line continues a multi-word name (e.g. "BEN" on line 1, "ALI" on line 2)
                    if (j + 1 < lines.size()) {
                        String nextLine = lines.get(j + 1);
                        if (!isAnyFieldLabel(nextLine) && !isLanguageDescriptorLine(nextLine)) {
                            String nextClean = cleanNameValue(nextLine);
                            if (nextClean != null && isValidName(nextClean) && isPureNameToken(nextClean)) {
                                return Optional.of(cleanedCandidate + " " + nextClean);
                            }
                        }
                    }
                    return Optional.of(cleanedCandidate);
                }
            }
        }

        // 3. Multiline regex fallback
        Pattern fallbackPattern = isFirstName ? CIN_PRENOM_MULTILINE : CIN_NOM_MULTILINE;
        Matcher m = fallbackPattern.matcher(text);
        if (m.find()) {
            String val = cleanNameValue(m.group(1));
            if (val != null && isValidName(val)) {
                return Optional.of(val);
            }
        }

        return Optional.empty();
    }

    private String stripNameLabelPrefix(String line, boolean isFirstName) {
        if (line == null) return "";
        if (isFirstName) {
            return line.replaceFirst(
                    "(?i)^.*?\\b(?:Pr[eé]noms?|Given\\s*names?|First\\s*names?|ال[إا]سم(?:\\s+الشخصي)?|الاسم)" +
                            "(?:\\s*\\([^)]{1,30}\\)|\\s*\\[[^\\]]{1,30}\\]|\\s+en\\s+(?:français|francais|arabe|anglais))?" +
                            "(?:\\s*[/\\\\|\\-—:]\\s*(?:Pr[eé]noms?|Given\\s*names?|First\\s*names?|ال[إا]سم(?:\\s+الشخصي)?|الاسم)" +
                            "(?:\\s*\\([^)]{1,30}\\)|\\s*\\[[^\\]]{1,30}\\]|\\s+en\\s+(?:français|francais|arabe|anglais))?)*" +
                            "\\s*[:\\-._—|~]*",
                    ""
            );
        } else {
            return line.replaceFirst(
                    "(?i)^.*?\\b(?:Nom(?:\\s+de\\s+famille)?|Surname|Last\\s*Name|Family\\s*Name|اللقب|النسب)" +
                            "(?:\\s*\\([^)]{1,30}\\)|\\s*\\[[^\\]]{1,30}\\]|\\s+en\\s+(?:français|francais|arabe|anglais))?" +
                            "(?:\\s*[/\\\\|\\-—:]\\s*(?:Nom(?:\\s+de\\s+famille)?|Surname|Last\\s*Name|Family\\s*Name|اللقب|النسب)" +
                            "(?:\\s*\\([^)]{1,30}\\)|\\s*\\[[^\\]]{1,30}\\]|\\s+en\\s+(?:français|francais|arabe|anglais))?)*" +
                            "\\s*[:\\-._—|~]*",
                    ""
            );
        }
    }

    private boolean isLanguageDescriptorLine(String line) {
        if (line == null) return false;
        String clean = line.trim();
        return clean.matches("(?i)^[\\(\\[\\{]?\\s*(?:en\\s+)?(?:français|francais|arabe|arabic|anglais|english|بالعربية|بالفرنسية)\\s*[\\)\\]\\}]?$");
    }

    private boolean isPureNameToken(String token) {
        if (token == null || token.length() < 2) return false;
        return token.matches("^[A-Za-zÀ-ÿ\u0600-\u06FF '\\-]+$");
    }

    private boolean isOtherFieldLabel(String line, boolean extractingFirstName) {
        String lower = line.toLowerCase();
        if (extractingFirstName) {
            if (containsNomLabel(line)) return true;
        } else {
            if (containsPrenomLabel(line)) return true;
        }
        return lower.contains("date") || lower.contains("lieu") || lower.contains("sexe") ||
                lower.contains("nationalit") || lower.contains("adresse") || lower.contains("passeport") ||
                lower.contains("passport") || lower.contains("تاريخ") || lower.contains("مكان") ||
                lower.contains("الجنس") || lower.contains("الجنسية") || lower.contains("العنوان") ||
                lower.contains("المهنة") || lower.contains("رقم") || lower.contains("n°") ||
                lower.contains("cin") || lower.contains("signature") || lower.contains("autorit");
    }

    private boolean containsNomLabel(String line) {
        if (line == null) return false;
        String lower = line.toLowerCase();
        if (lower.contains("أم") || lower.contains("ام") || lower.contains("mère") || lower.contains("mere") || lower.contains("mother")) return false;
        if (lower.contains("أب") || lower.contains("اب") || lower.contains("père") || lower.contains("pere") || lower.contains("father")) return false;
        if (lower.contains("جمهورية") || lower.contains("republique")) return false;
        if (lower.contains("بطاقة") || lower.contains("carte")) return false;
        return NOM_LABEL_REGEX.matcher(line).find();
    }

    private boolean containsPrenomLabel(String line) {
        if (line == null) return false;
        String lower = line.toLowerCase();
        if (lower.contains("أب") || lower.contains("اب") || lower.contains("pere") || lower.contains("père") || lower.contains("father")) return false;
        if (lower.contains("جد") || lower.contains("grand")) return false;
        if (lower.contains("أم") || lower.contains("ام") || lower.contains("mere") || lower.contains("mère") || lower.contains("mother")) return false;
        return PRENOM_LABEL_REGEX.matcher(line).find();
    }

    private boolean isAnyFieldLabel(String line) {
        String lower = line.toLowerCase();
        return lower.contains("nom") || lower.contains("prénom") || lower.contains("prenom") ||
                lower.contains("surname") || lower.contains("given") || lower.contains("date") ||
                lower.contains("lieu") || lower.contains("sexe") || lower.contains("nationalit") ||
                lower.contains("adresse") || lower.contains("passeport") || lower.contains("passport") ||
                lower.contains("اللقب") || lower.contains("الاسم") || lower.contains("الإسم") ||
                lower.contains("تاريخ") || lower.contains("مكان") || lower.contains("الجنس") ||
                lower.contains("الجنسية") || lower.contains("العنوان") || lower.contains("المهنة") ||
                lower.contains("رقم") || lower.contains("n°");
    }

    private String cleanNameValue(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // 1. Remove bracketed / parenthetical language or metadata tags like (en français), (بالفرنسية)
        s = s.replaceAll("(?i)\\([^)]{0,35}(?:français|francais|arabe|arabic|anglais|english|عربي|فرنسي|بالعربية|بالفرنسية)[^)]{0,35}\\)", " ");
        s = s.replaceAll("(?i)\\[[^\\]]{0,35}(?:français|francais|arabe|arabic|anglais|english|عربي|فرنسي|بالعربية|بالفرنسية)[^\\]]{0,35}\\]", " ");

        // 2. Remove standalone language qualifiers e.g. "en français", "en arabe"
        s = s.replaceAll("(?i)\\b(?:en\\s+(?:français|francais|arabe|anglais))\\b", " ");

        // 3. Remove known trailing label words if any got appended on the same line
        s = s.replaceAll("(?i)\\s*\\b(Pr[eé]noms?|Given\\s+names?|Nom(?:\\s+de\\s+famille)?|Surname|Last\\s*Name|First\\s*Name|Date|Sexe|Lieu|Nationalit[eé]|N°|CIN|الاسم|الإسم|اللقب|النسب|تاريخ|الجنسية|المهنة|العنوان|رقم)\\b.*$", "");

        // 4. Strip punctuation and noise symbols from boundaries
        s = s.replaceAll("^[\\s:;\\-_/\\\\|~.*«»<>()\\[\\]#{}\"?!=+]+", "");
        s = s.replaceAll("[\\s:;\\-_/\\\\|~.*«»<>()\\[\\]#{}\"?!=+]+$", "");
        s = s.replaceAll("\\s{2,}", " ").trim();

        return s;
    }

    private static final Set<String> INVALID_NAME_WORDS = Set.of(
            "S", "C", "X", "N", "M", "F", "V", "O", "I", "L", "A", "B", "D", "E", "T", "R",
            "EN", "DE", "DU", "LA", "LE", "DES", "LES", "ET", "OU", "AU", "AUX",
            "FRANCAIS", "FRANÇAIS", "FRENCH", "ARABE", "ARABIC", "ANGLAIS", "ENGLISH",
            "NOM", "PRENOM", "PRENOMS", "PRÉNOM", "PRÉNOMS", "SURNAME", "NAME", "NAMES", "GIVEN",
            "CARTE", "CIN", "PASSEPORT", "PASSPORT", "REPUBLIQUE", "TUNISIENNE", "TUNISIA",
            "IDENTITY", "CARD", "NATIONAL", "NATIONALE", "SIGNATURE", "AUTORITE",
            "TITULAIRE", "HOLDER", "BEARER",
            "اللقب", "النسب", "الاسم", "الإسم", "الشخصي", "العائلي", "العربية", "الفرنسية",
            "بطاقة", "التعريف", "الوطنية", "جواز", "السفر", "تونس", "تونسية", "الجمهورية",
            "إمضاء", "صاحب", "البطاقة"
    );

    private boolean isValidName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.length() < 2) return false;

        // Strip boundary noise
        String clean = trimmed.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
        if (clean.length() < 2) return false;

        // Must have at least 2 alphabetic letters (never return a single character like 's')
        long letterCount = clean.chars().filter(Character::isLetter).count();
        if (letterCount < 2) return false;

        // Check if the entire string or any single token is just an invalid descriptor word
        String normUpper = normalizer.normalizeName(clean).toUpperCase();
        if (normUpper.isEmpty() || INVALID_NAME_WORDS.contains(normUpper)) {
            return false;
        }

        // Multi-word descriptor phrases
        if (normUpper.equals("EN FRANCAIS") || normUpper.equals("EN ARABE") || normUpper.equals("EN ANGLAIS")
                || normUpper.equals("REPUBLIQUE TUNISIENNE") || normUpper.equals("CARTE NATIONALE")
                || normUpper.equals("CARTE IDENTITE") || normUpper.equals("PASSEPORT TUNISIEN")) {
            return false;
        }

        // Must contain valid Latin or Arabic letters
        return clean.matches(".*[A-Za-zÀ-ÿ\u0600-\u06FF].*");
    }

    // ── Certificat d'immatriculation / Registre du Commerce ──────────────────

    private Map<String, String> extractCertificatImmatriculationFields(String text) {
        Map<String, String> f = new LinkedHashMap<>();
        // Especially: Date de début d'activité, Dénomination sociale, Forme juridique, Siège social, Secteur d'activité
        findFirst(DATE_PATTERN,               text).ifPresent(v -> f.put("registrationDate",   v));
        findFirst(COMPANY_NAME_PATTERN,       text).ifPresent(v -> f.put("companyName",        clean(v)));
        findFirst(BUSINESS_TYPE_PATTERN,      text).ifPresent(v -> f.put("businessType",       clean(v)));
        findFirst(ADDRESS_PATTERN,            text).ifPresent(v -> f.put("address",            clean(v)));
        findFirst(SECTOR_PATTERN,             text).ifPresent(v -> f.put("sector",             clean(v)));

        // Additional helpful identifiers if found
        findFirst(FISCAL_NUMBER_PATTERN,      text).ifPresent(v -> f.put("fiscalNumber",       v));
        findFirst(REGISTRATION_NUMBER_PATTERN,text).ifPresent(v -> f.put("registrationNumber", v));
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
            return List.of("passportNumber", "lastName", "firstName");
        }
        if (upper.contains("IMMATRICULATION") || upper.equals("PATENTE") ||
                upper.equals("COMMERCIAL_REGISTER")) {
            return List.of("companyName", "registrationDate");
        }
        // CIN
        return List.of("cinNumber", "lastName", "firstName");
    }
}
