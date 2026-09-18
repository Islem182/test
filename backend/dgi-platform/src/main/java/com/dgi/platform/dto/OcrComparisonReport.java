package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Full comparison report returned by POST /api/ocr/compare-fields.
 *
 * JSON shape:
 * {
 *   "overallStatus": "MATCH",
 *   "overallScore": 96,
 *   "documentType": "CIN",
 *   "summaryMessage": "All fields matched successfully.",
 *   "requiresAgentReview": false,
 *   "fields": {
 *     "firstName":  { "status": "MATCH",  "score": 95, ... },
 *     "lastName":   { "status": "MATCH",  "score": 94, ... },
 *     "cinNumber":  { "status": "MATCH",  "score": 100, ... },
 *     "birthDate":  { "status": "MATCH",  "score": 100, ... },
 *     "address":    { "status": "POSSIBLE_MATCH", "score": 72, ... }
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrComparisonReport {

    /**
     * Overall decision across all compared fields.
     * MATCH | POSSIBLE_MATCH | MANUAL_REVIEW | MISMATCH
     *
     * Derived as the worst status among all compared fields,
     * with document-number MISMATCH always propagating to overall MISMATCH.
     */
    private String overallStatus;

    /**
     * Weighted average score (0–100) across all compared fields.
     * Document number and date fields are weighted 2× vs name fields.
     */
    private int overallScore;

    /** CIN | PASSPORT (echoed from the request) */
    private String documentType;

    /** Human-readable one-sentence verdict for display */
    private String summaryMessage;

    /**
     * True when any field is MANUAL_REVIEW or when overallStatus is
     * POSSIBLE_MATCH or MISMATCH. The final identity decision always
     * rests with an authorized DGI agent.
     */
    private boolean requiresAgentReview;

    /**
     * Per-field results, keyed by field name
     * (cinNumber | passportNumber | firstName | lastName | birthDate | address | nationality).
     */
    private Map<String, FieldComparisonResult> fields;

    /**
     * Fields that could not be compared because either the user value
     * or the OCR value was absent/empty.
     */
    private List<String> skippedFields;
}
