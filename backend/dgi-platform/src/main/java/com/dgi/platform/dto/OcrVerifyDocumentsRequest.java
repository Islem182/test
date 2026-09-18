package com.dgi.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for POST /api/ocr/verify-documents.
 *
 * The frontend sends the three extractedData maps it already received
 * from the /api/ocr/extract-document calls in Step 3.
 * No file upload is needed here — verification is pure data comparison.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrVerifyDocumentsRequest {

    /**
     * extractedData from the CIN OCR response.
     * Expected keys: cinNumber, firstName, lastName, birthDate, address
     */
    @NotNull(message = "cinData must not be null")
    private Map<String, String> cinData;

    /**
     * extractedData from the Passport OCR response.
     * Expected keys: passportNumber, firstName, lastName, birthDate, nationality
     */
    @NotNull(message = "passportData must not be null")
    private Map<String, String> passportData;

    /**
     * extractedData from the Certificat d'immatriculation OCR response.
     * Expected keys: companyName, businessType, registrationDate, address, fiscalNumber, etc.
     */
    @NotNull(message = "certificatData must not be null")
    private Map<String, String> certificatData;
}
