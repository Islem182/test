package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Unified OCR response returned by both the extract-document endpoint
 * and the legacy compare endpoint.
 *
 * extractedData contains flat key-value pairs:
 *   CIN  → cinNumber, firstName, lastName, birthDate, address
 *   PASS → passportNumber, firstName, lastName, nationality
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrVerificationResponse {

    /** SUCCESS | FAILED | PARTIAL */
    private String verification;

    /** 0.0 – 1.0 Tesseract mean confidence / match ratio */
    private double confidenceScore;

    /** CIN | PASSPORT (echoed from the request) */
    private String documentType;

    /**
     * Structured fields extracted from the document.
     * Keys depend on documentType (see class javadoc).
     */
    private Map<String, String> extractedData;

    /**
     * Field-level mismatches (populated by the /compare endpoint only).
     * Empty list means all compared fields matched.
     */
    private List<String> differences;

    /**
     * Human-readable status message — useful when OCR could not
     * extract some fields or the image quality was poor.
     */
    private String statusMessage;
}
