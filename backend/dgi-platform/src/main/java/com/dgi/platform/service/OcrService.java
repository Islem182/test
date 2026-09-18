package com.dgi.platform.service;

import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * Run real Tesseract OCR on an uploaded image file and return
     * structured extracted fields.
     *
     * @param file         JPG / JPEG / PNG image uploaded by the user
     * @param documentType "CIN" or "PASSPORT"
     * @return structured OCR result with extracted identity fields
     */
    OcrVerificationResponse extractFromUpload(MultipartFile file, String documentType);

    /**
     * Legacy path-based analysis (kept for the /compare endpoint).
     * In the real implementation this is a thin wrapper that delegates
     * to a pre-built result; it does NOT perform live file reading.
     */
    OcrVerificationResponse analyzeDocument(String documentPath, String documentType);

    /**
     * Compare OCR-extracted data against user-supplied expected values.
     */
    OcrVerificationResponse compareOcrWithUserData(OcrCompareRequest request);
}
