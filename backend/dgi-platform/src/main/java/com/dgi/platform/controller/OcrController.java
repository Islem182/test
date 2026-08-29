package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;
import com.dgi.platform.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Tag(name = "AI OCR Engine", description = "Optical Character Recognition for identity and tax document verification")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze uploaded document and extract structured text fields via OCR")
    public ResponseEntity<ApiResponse<OcrVerificationResponse>> analyzeDocument(
            @RequestParam String documentPath,
            @RequestParam(defaultValue = "CIN") String documentType) {

        OcrVerificationResponse response = ocrService.analyzeDocument(documentPath, documentType);
        return ResponseEntity.ok(ApiResponse.success("OCR analysis completed successfully", response));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare OCR extracted data against user provided registration fields")
    public ResponseEntity<ApiResponse<OcrVerificationResponse>> compareUserData(
            @Valid @RequestBody OcrCompareRequest request) {

        OcrVerificationResponse response = ocrService.compareOcrWithUserData(request);
        return ResponseEntity.ok(ApiResponse.success("OCR field comparison completed", response));
    }
}
