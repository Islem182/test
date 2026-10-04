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

    /** Boolean flag: true when all verified fields match and identity is consistent */
    private boolean verified;

    /** MATCH | POSSIBLE_MATCH | MISMATCH | UNABLE_TO_COMPARE */
    private String identityConsistency;

    /** Human-readable one-sentence verdict */
    private String summary;

    public CinSection getCin() {
        return cin;
    }

    public void setCin(CinSection cin) {
        this.cin = cin;
    }

    public PassportSection getPassport() {
        return passport;
    }

    public void setPassport(PassportSection passport) {
        this.passport = passport;
    }

    public CertificatSection getCommercialRegistration() {
        return commercialRegistration;
    }

    public void setCommercialRegistration(CertificatSection commercialRegistration) {
        this.commercialRegistration = commercialRegistration;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean getVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getIdentityConsistency() {
        return identityConsistency;
    }

    public void setIdentityConsistency(String identityConsistency) {
        this.identityConsistency = identityConsistency;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public static OcrVerificationReportBuilder builder() {
        return new OcrVerificationReportBuilder();
    }

    public static class OcrVerificationReportBuilder {
        private CinSection cin;
        private PassportSection passport;
        private CertificatSection commercialRegistration;
        private String overallStatus;
        private boolean verified;
        private String identityConsistency;
        private String summary;

        public OcrVerificationReportBuilder cin(CinSection cin) {
            this.cin = cin;
            return this;
        }

        public OcrVerificationReportBuilder passport(PassportSection passport) {
            this.passport = passport;
            return this;
        }

        public OcrVerificationReportBuilder commercialRegistration(CertificatSection commercialRegistration) {
            this.commercialRegistration = commercialRegistration;
            return this;
        }

        public OcrVerificationReportBuilder overallStatus(String overallStatus) {
            this.overallStatus = overallStatus;
            return this;
        }

        public OcrVerificationReportBuilder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public OcrVerificationReportBuilder isVerified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public OcrVerificationReportBuilder identityConsistency(String identityConsistency) {
            this.identityConsistency = identityConsistency;
            return this;
        }

        public OcrVerificationReportBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public OcrVerificationReport build() {
            return new OcrVerificationReport(cin, passport, commercialRegistration, overallStatus, verified, identityConsistency, summary);
        }
    }

    // ── Nested section classes ────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CinSection {
        private OcrFieldResult numeroCin;
        private OcrFieldResult nom;
        private OcrFieldResult prenom;
        private OcrFieldResult adresse;
        /** VALID | INVALID | PARTIAL */
        private String documentStatus;

        public OcrFieldResult getNumeroCin() { return numeroCin; }
        public void setNumeroCin(OcrFieldResult numeroCin) { this.numeroCin = numeroCin; }
        public OcrFieldResult getNom() { return nom; }
        public void setNom(OcrFieldResult nom) { this.nom = nom; }
        public OcrFieldResult getPrenom() { return prenom; }
        public void setPrenom(OcrFieldResult prenom) { this.prenom = prenom; }
        public OcrFieldResult getAdresse() { return adresse; }
        public void setAdresse(OcrFieldResult adresse) { this.adresse = adresse; }
        public String getDocumentStatus() { return documentStatus; }
        public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }

        public static CinSectionBuilder builder() {
            return new CinSectionBuilder();
        }

        public static class CinSectionBuilder {
            private OcrFieldResult numeroCin;
            private OcrFieldResult nom;
            private OcrFieldResult prenom;
            private OcrFieldResult adresse;
            private String documentStatus;

            public CinSectionBuilder numeroCin(OcrFieldResult numeroCin) { this.numeroCin = numeroCin; return this; }
            public CinSectionBuilder nom(OcrFieldResult nom) { this.nom = nom; return this; }
            public CinSectionBuilder prenom(OcrFieldResult prenom) { this.prenom = prenom; return this; }
            public CinSectionBuilder adresse(OcrFieldResult adresse) { this.adresse = adresse; return this; }
            public CinSectionBuilder documentStatus(String documentStatus) { this.documentStatus = documentStatus; return this; }

            public CinSection build() {
                return new CinSection(numeroCin, nom, prenom, adresse, documentStatus);
            }
        }
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

        public OcrFieldResult getNumeroPassport() { return numeroPassport; }
        public void setNumeroPassport(OcrFieldResult numeroPassport) { this.numeroPassport = numeroPassport; }
        public OcrFieldResult getNom() { return nom; }
        public void setNom(OcrFieldResult nom) { this.nom = nom; }
        public OcrFieldResult getPrenom() { return prenom; }
        public void setPrenom(OcrFieldResult prenom) { this.prenom = prenom; }
        public String getIdentityCompatibility() { return identityCompatibility; }
        public void setIdentityCompatibility(String identityCompatibility) { this.identityCompatibility = identityCompatibility; }
        public int getCompatibilityScore() { return compatibilityScore; }
        public void setCompatibilityScore(int compatibilityScore) { this.compatibilityScore = compatibilityScore; }
        public String getDocumentStatus() { return documentStatus; }
        public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }

        public static PassportSectionBuilder builder() {
            return new PassportSectionBuilder();
        }

        public static class PassportSectionBuilder {
            private OcrFieldResult numeroPassport;
            private OcrFieldResult nom;
            private OcrFieldResult prenom;
            private String identityCompatibility;
            private int compatibilityScore;
            private String documentStatus;

            public PassportSectionBuilder numeroPassport(OcrFieldResult numeroPassport) { this.numeroPassport = numeroPassport; return this; }
            public PassportSectionBuilder nom(OcrFieldResult nom) { this.nom = nom; return this; }
            public PassportSectionBuilder prenom(OcrFieldResult prenom) { this.prenom = prenom; return this; }
            public PassportSectionBuilder identityCompatibility(String identityCompatibility) { this.identityCompatibility = identityCompatibility; return this; }
            public PassportSectionBuilder compatibilityScore(int compatibilityScore) { this.compatibilityScore = compatibilityScore; return this; }
            public PassportSectionBuilder documentStatus(String documentStatus) { this.documentStatus = documentStatus; return this; }

            public PassportSection build() {
                return new PassportSection(numeroPassport, nom, prenom, identityCompatibility, compatibilityScore, documentStatus);
            }
        }
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

        public OcrFieldResult getDateDebutActivite() { return dateDebutActivite; }
        public void setDateDebutActivite(OcrFieldResult dateDebutActivite) { this.dateDebutActivite = dateDebutActivite; }
        public OcrFieldResult getDenominationSociale() { return denominationSociale; }
        public void setDenominationSociale(OcrFieldResult denominationSociale) { this.denominationSociale = denominationSociale; }
        public OcrFieldResult getFormeJuridique() { return formeJuridique; }
        public void setFormeJuridique(OcrFieldResult formeJuridique) { this.formeJuridique = formeJuridique; }
        public OcrFieldResult getSiegeSocial() { return siegeSocial; }
        public void setSiegeSocial(OcrFieldResult siegeSocial) { this.siegeSocial = siegeSocial; }
        public OcrFieldResult getSecteurActivite() { return secteurActivite; }
        public void setSecteurActivite(OcrFieldResult secteurActivite) { this.secteurActivite = secteurActivite; }
        public String getDocumentStatus() { return documentStatus; }
        public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }

        public static CertificatSectionBuilder builder() {
            return new CertificatSectionBuilder();
        }

        public static class CertificatSectionBuilder {
            private OcrFieldResult dateDebutActivite;
            private OcrFieldResult denominationSociale;
            private OcrFieldResult formeJuridique;
            private OcrFieldResult siegeSocial;
            private OcrFieldResult secteurActivite;
            private String documentStatus;

            public CertificatSectionBuilder dateDebutActivite(OcrFieldResult dateDebutActivite) { this.dateDebutActivite = dateDebutActivite; return this; }
            public CertificatSectionBuilder denominationSociale(OcrFieldResult denominationSociale) { this.denominationSociale = denominationSociale; return this; }
            public CertificatSectionBuilder formeJuridique(OcrFieldResult formeJuridique) { this.formeJuridique = formeJuridique; return this; }
            public CertificatSectionBuilder siegeSocial(OcrFieldResult siegeSocial) { this.siegeSocial = siegeSocial; return this; }
            public CertificatSectionBuilder secteurActivite(OcrFieldResult secteurActivite) { this.secteurActivite = secteurActivite; return this; }
            public CertificatSectionBuilder documentStatus(String documentStatus) { this.documentStatus = documentStatus; return this; }

            public CertificatSection build() {
                return new CertificatSection(dateDebutActivite, denominationSociale, formeJuridique, siegeSocial, secteurActivite, documentStatus);
            }
        }
    }
}
