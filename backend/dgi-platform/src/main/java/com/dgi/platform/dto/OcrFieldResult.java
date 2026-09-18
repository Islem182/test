package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result for a single extracted field inside a document verification section.
 *
 * status values:
 *   VALID   — field was found and contains a non-empty value
 *   MISSING — field could not be extracted from the document
 *   INVALID — field was found but does not pass a format/content check
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrFieldResult {

    /** The extracted value (null when status is MISSING). */
    private String value;

    /** VALID | MISSING | INVALID */
    private String status;

    /** Optional human-readable explanation when status != VALID. */
    private String message;
}
