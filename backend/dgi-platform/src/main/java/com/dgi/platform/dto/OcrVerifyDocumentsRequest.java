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
     * Expected keys: companyName, businessType, registrationDate, address, sector
     */
    @NotNull(message = "certificatData must not be null")
    private Map<String, String> certificatData;

    /**
     * Data entered by the user during Step 1 (Personal Info) and Step 2 (Commercial Activity).
     * Expected keys:
     * Step 1: firstName, lastName, cin, passport, phone, address
     * Step 2: businessName, businessType, sector, startDate, businessAddress
     */
    private Map<String, String> userEnteredData;

    public Map<String, String> getCinData() {
        return cinData;
    }

    public void setCinData(Map<String, String> cinData) {
        this.cinData = cinData;
    }

    public Map<String, String> getPassportData() {
        return passportData;
    }

    public void setPassportData(Map<String, String> passportData) {
        this.passportData = passportData;
    }

    public Map<String, String> getCertificatData() {
        return certificatData;
    }

    public void setCertificatData(Map<String, String> certificatData) {
        this.certificatData = certificatData;
    }

    public Map<String, String> getUserEnteredData() {
        return userEnteredData;
    }

    public void setUserEnteredData(Map<String, String> userEnteredData) {
        this.userEnteredData = userEnteredData;
    }

    public static OcrVerifyDocumentsRequestBuilder builder() {
        return new OcrVerifyDocumentsRequestBuilder();
    }

    public static class OcrVerifyDocumentsRequestBuilder {
        private Map<String, String> cinData;
        private Map<String, String> passportData;
        private Map<String, String> certificatData;
        private Map<String, String> userEnteredData;

        public OcrVerifyDocumentsRequestBuilder cinData(Map<String, String> cinData) {
            this.cinData = cinData;
            return this;
        }

        public OcrVerifyDocumentsRequestBuilder passportData(Map<String, String> passportData) {
            this.passportData = passportData;
            return this;
        }

        public OcrVerifyDocumentsRequestBuilder certificatData(Map<String, String> certificatData) {
            this.certificatData = certificatData;
            return this;
        }

        public OcrVerifyDocumentsRequestBuilder userEnteredData(Map<String, String> userEnteredData) {
            this.userEnteredData = userEnteredData;
            return this;
        }

        public OcrVerifyDocumentsRequest build() {
            return new OcrVerifyDocumentsRequest(cinData, passportData, certificatData, userEnteredData);
        }
    }
}
