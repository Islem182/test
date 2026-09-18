package com.dgi.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for POST /api/ocr/compare-fields.
 *
 * Both maps use the same key set as OcrVerificationResponse.extractedData:
 *   CIN      keys: cinNumber, firstName, lastName, birthDate, address
 *   PASSPORT keys: passportNumber, firstName, lastName, birthDate, nationality
 *
 * The frontend sends:
 *   userEnteredData  — values typed into the Step-1 form by the taxpayer
 *   ocrExtractedData — values from the previous OCR extraction response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrComparisonRequest {

    /** CIN or PASSPORT */
    @NotBlank(message = "documentType is required")
    private String documentType;

    /**
     * Values entered manually by the taxpayer in the registration form.
     * Keys: cinNumber | passportNumber | firstName | lastName | birthDate | address | nationality
     */
    @NotNull(message = "userEnteredData must not be null")
    private Map<String, String> userEnteredData;

    /**
     * Values extracted by the OCR engine in the previous step.
     * Same key set as userEnteredData.
     */
    @NotNull(message = "ocrExtractedData must not be null")
    private Map<String, String> ocrExtractedData;
}
