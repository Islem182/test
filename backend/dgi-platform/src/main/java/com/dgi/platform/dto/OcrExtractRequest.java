package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata accompanying the multipart file upload to the OCR endpoint.
 * The actual image bytes arrive as a MultipartFile; this DTO carries
 * the document type declared by the uploader.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrExtractRequest {

    /**
     * Document category chosen by the user: CIN or PASSPORT.
     * Drives which regex patterns the OCR service uses for field extraction.
     */
    private String documentType; // CIN | PASSPORT
}
