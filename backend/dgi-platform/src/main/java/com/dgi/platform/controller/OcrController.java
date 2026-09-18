package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrComparisonReport;
import com.dgi.platform.dto.OcrComparisonRequest;
import com.dgi.platform.dto.OcrVerificationReport;
import com.dgi.platform.dto.OcrVerificationResponse;
import com.dgi.platform.dto.OcrVerifyDocumentsRequest;
import com.dgi.platform.service.OcrComparisonService;
import com.dgi.platform.service.OcrService;
import com.dgi.platform.service.OcrVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR Engine", description = "Optical Character Recognition — upload CIN / Passport images and extract identity fields")
public class OcrController {

    private final OcrService ocrService;
    private final OcrVerificationService ocrVerificationService;

    // -------------------------------------------------------------------------
    // Primary endpoint — real multipart file upload + Tesseract OCR
    // -------------------------------------------------------------------------

    /**
     * POST /api/ocr/extract-document
     *
     * Accepts a JPG/PNG scan of a Tunisian CIN card or Passport.
     * Runs real Tesseract OCR in-memory and returns structured extracted fields.
     * The file is never written to disk and is discarded after processing.
     *
     * Form-data parameters:
     *   file         — the image file (required)
     *   documentType — "CIN" or "PASSPORT" (required)
     */
    @PostMapping(value = "/extract-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload a CIN or Passport image and extract identity fields via OCR",
        description = "Accepts JPG/JPEG/PNG files up to 10 MB. " +
                      "Returns extracted fields: cinNumber or passportNumber, firstName, " +
                      "lastName, birthDate, address (CIN) or nationality (Passport). " +
                      "The uploaded file is processed in memory and never stored."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "OCR completed — check verification field for SUCCESS / PARTIAL",
            content = @Content(schema = @Schema(implementation = OcrVerificationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid file type, file too large, or unreadable image"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        )
    })
    public ResponseEntity<ApiResponse<OcrVerificationResponse>> extractDocument(
            @Parameter(description = "CIN or Passport image (JPG/JPEG/PNG, max 10 MB)", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "Document type: CIN or PASSPORT", required = true)
            @RequestParam(value = "documentType", defaultValue = "CIN") String documentType) {

        log.info("OCR extract-document request — documentType={}", documentType);
        OcrVerificationResponse response = ocrService.extractFromUpload(file, documentType);
        return ResponseEntity.ok(ApiResponse.success("OCR extraction completed", response));
    }

    // -------------------------------------------------------------------------
    // Step 4 endpoint — structured per-document verification report
    // -------------------------------------------------------------------------

    /**
     * POST /api/ocr/verify-documents
     *
     * Accepts the three extractedData maps already produced in Step 3
     * (no file upload needed here) and returns a structured verification report:
     *   - CIN  : numeroCin, nom, prénom, adresse
     *   - Passport : numeroPassport, nom, prénom + identity compatibility with CIN
     *   - Certificat : dateDebutActivite, denominationSociale, formeJuridique,
     *                  siegeSocial, secteurActivite
     *   - overallStatus / identityConsistency / summary
     */
    @PostMapping("/verify-documents")
    @Operation(
        summary = "Verify and cross-check the three OCR-extracted document maps (Step 4)",
        description = "Send the extractedData maps from the three /extract-document responses. " +
                      "Returns VALID/MISSING/INVALID per field, MATCH/POSSIBLE_MATCH/MISMATCH " +
                      "for CIN↔Passport identity, and an overall document status."
    )
    public ResponseEntity<ApiResponse<OcrVerificationReport>> verifyDocuments(
            @Valid @RequestBody OcrVerifyDocumentsRequest request) {

        log.info("OCR verify-documents request received");
        OcrVerificationReport report = ocrVerificationService.buildReport(request);
        return ResponseEntity.ok(ApiResponse.success("Verification report generated", report));
    }

    // -------------------------------------------------------------------------
    // Legacy endpoints — kept for backward compatibility
    // -------------------------------------------------------------------------

    @PostMapping("/analyze")
    @Operation(
        summary = "[Legacy] Analyze document by path — use /extract-document for real OCR",
        description = "This endpoint no longer performs live file reading. " +
                      "Use POST /api/ocr/extract-document with a real image upload instead."
    )
    public ResponseEntity<ApiResponse<OcrVerificationResponse>> analyzeDocument(
            @RequestParam String documentPath,
            @RequestParam(defaultValue = "CIN") String documentType) {

        OcrVerificationResponse response = ocrService.analyzeDocument(documentPath, documentType);
        return ResponseEntity.ok(ApiResponse.success("OCR analysis completed", response));
    }

    @PostMapping("/compare")
    @Operation(
        summary = "Compare previously extracted OCR data against user-supplied expected values",
        description = "Supply the extracted field values alongside expected values to get a " +
                      "field-by-field match report."
    )
    public ResponseEntity<ApiResponse<OcrVerificationResponse>> compareUserData(
            @Valid @RequestBody OcrCompareRequest request) {

        OcrVerificationResponse response = ocrService.compareOcrWithUserData(request);
        return ResponseEntity.ok(ApiResponse.success("OCR comparison completed", response));
    }
}
