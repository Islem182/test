package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of comparing a single identity field between user input and OCR extraction.
 *
 * Status rules:
 *  MATCH          — score ≥ 95  (exact or near-exact after normalization)
 *  POSSIBLE_MATCH — score 70–94 (same name, different romanization variant)
 *  MANUAL_REVIEW  — score 50–69 (ambiguous / poor OCR quality)
 *  MISMATCH       — score < 50  (clearly different values)
 *
 * For document numbers and dates, only MATCH or MISMATCH are used (no fuzzy).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldComparisonResult {

    /** Field key matching extractedData keys: cinNumber, firstName, lastName, birthDate, address, passportNumber */
    private String fieldKey;

    /** Human-readable field label for display (e.g. "First Name / الاسم") */
    private String fieldLabel;

    /** Value as entered by the user (raw, before normalization) */
    private String userValue;

    /** Value extracted by OCR (raw, before normalization) */
    private String ocrValue;

    /** MATCH | POSSIBLE_MATCH | MANUAL_REVIEW | MISMATCH */
    private String status;

    /** 0–100 integer confidence score */
    private int score;

    /**
     * Human-readable explanation shown to the taxpayer/agent.
     * Null when status is MATCH.
     */
    private String message;

    /**
     * Comparison strategy used for this field.
     * One of: EXACT, FUZZY_NAME, DATE, ADDRESS
     */
    private String strategy;
}
