package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full structured verification report returned by POST /api/ocr/verify-documents.
 *
 * Example response:
 * {
 *   "cin": {
 *     "numeroCin":  { "value": "12345678",   "status": "VALID" },
 *     "nom":        { "value": "BEN ALI",     "status": "VALID" },
 *     "prenom":     { "value": "MOHAMED",     "status": "VALID" },
 *     "adresse":    { "value": "12 Rue ...",  "status": "VALID" },
 *     "documentStatus": "VALID"
 *   },
 *   "passport": {
 *     "numeroPassport": { "value": "AB123456", "status": "VALID" },
 *     "nom":            { "value": "BEN ALI",  "status": "VALID" },
 *     "prenom":         { "value": "Mohamed",  "status": "VALID" },
 *     "identityCompatibility": "MATCH",
 *     "compatibilityScore": 97,
 *     "documentStatus": "VALID"
 *   },
 *   "commercialRegistration": {
 *     "dateDebutActivite":  { "value": "2024-01-15", "status": "VALID" },
 *     "denominationSociale":{ "value": "TechCo SARL","status": "VALID" },
 *     "formeJuridique":     { "value": "SARL",       "status": "VALID" },
 *     "siegeSocial":        { "value": "Ariana",     "status": "VALID" },
 *     "secteurActivite":    { "value": "IT",         "status": "VALID" },
 *     "documentStatus": "VALID"
 *   },
 *   "overallStatus": "VALID",
 *   "identityConsistency": "MATCH",
 *   "summary": "All documents verified successfully."
 * }
 *
 * overallStatus / documentStatus values:  VALID | INVALID | PARTIAL
 * identityCompatibility values:           MATCH | POSSIBLE_MATCH | MISMATCH | UNABLE_TO_COMPARE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrVerificationReport {

    private CinSection           cin;
    private PassportSection      passport;
    private CertificatSection    commercialRegistration;

    /** VALID | INVALID | PARTIAL — worst status across all three documents */
    private String overallStatus;

    /** MATCH | POSSIBLE_MATCH | MISMATCH | UNABLE_TO_COMPARE */
    private String identityConsistency;

    /** Human-readable one-sentence verdict */
    private String summary;

    // ── Nested section classes ────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CinSection {
        private OcrFieldResult numeroCin;
        private OcrFieldResult nom;
        private OcrFieldResult prenom;
        private OcrFieldResult adresse;
        /** VALID | INVALID | PARTIAL */
        private String documentStatus;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PassportSection {
        private OcrFieldResult numeroPassport;
        private OcrFieldResult nom;
        private OcrFieldResult prenom;
        /** MATCH | POSSIBLE_MATCH | MISMATCH | UNABLE_TO_COMPARE */
        private String identityCompatibility;
        /** 0–100 — similarity score between CIN and Passport names */
        private int compatibilityScore;
        /** VALID | INVALID | PARTIAL */
        private String documentStatus;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CertificatSection {
        private OcrFieldResult dateDebutActivite;
        private OcrFieldResult denominationSociale;
        private OcrFieldResult formeJuridique;
        private OcrFieldResult siegeSocial;
        private OcrFieldResult secteurActivite;
        /** VALID | INVALID | PARTIAL */
        private String documentStatus;
    }
}
