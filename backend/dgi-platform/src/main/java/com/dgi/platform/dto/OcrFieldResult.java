package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result for a single extracted field inside a document verification section.
 *
 * status values:
 *   MATCH     — document value matches user input
 *   MISMATCH  — document value differs from user input
 *   NOT_FOUND — OCR could not find the field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrFieldResult {

    /** Name of the field (e.g., "Numéro CIN", "Nom", "Prénom", etc.) */
    private String fieldName;

    /** The extracted value from the document (null or empty when status is NOT_FOUND). */
    private String extractedValue;

    /** The value entered by the user in Step 1 or Step 2. */
    private String enteredValue;

    /** MATCH | MISMATCH | NOT_FOUND */
    private String status;

    /** Optional human-readable explanation of the comparison result. */
    private String message;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getExtractedValue() {
        return extractedValue;
    }

    public void setExtractedValue(String extractedValue) {
        this.extractedValue = extractedValue;
    }

    public String getEnteredValue() {
        return enteredValue;
    }

    public void setEnteredValue(String enteredValue) {
        this.enteredValue = enteredValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Backward-compatible getter for legacy code calling .getValue().
     */
    public String getValue() {
        return extractedValue;
    }

    /**
     * Backward-compatible setter for legacy code calling .setValue().
     */
    public void setValue(String value) {
        this.extractedValue = value;
    }

    public static OcrFieldResultBuilder builder() {
        return new OcrFieldResultBuilder();
    }

    public static class OcrFieldResultBuilder {
        private String fieldName;
        private String extractedValue;
        private String enteredValue;
        private String status;
        private String message;

        public OcrFieldResultBuilder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public OcrFieldResultBuilder extractedValue(String extractedValue) {
            this.extractedValue = extractedValue;
            return this;
        }

        public OcrFieldResultBuilder enteredValue(String enteredValue) {
            this.enteredValue = enteredValue;
            return this;
        }

        public OcrFieldResultBuilder value(String value) {
            this.extractedValue = value;
            return this;
        }

        public OcrFieldResultBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OcrFieldResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public OcrFieldResult build() {
            return new OcrFieldResult(fieldName, extractedValue, enteredValue, status, message);
        }
    }
}
