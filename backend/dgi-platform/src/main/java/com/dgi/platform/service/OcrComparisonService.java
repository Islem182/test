package com.dgi.platform.service;

import com.dgi.platform.dto.OcrComparisonReport;
import com.dgi.platform.dto.OcrComparisonRequest;

/**
 * Compares user-entered identity data against OCR-extracted values using
 * appropriate per-field strategies (exact, fuzzy name, date, address).
 *
 * This interface is separate from OcrService so it can be independently
 * tested, extended (e.g. ML-based scoring), or swapped without touching
 * the OCR extraction pipeline.
 */
public interface OcrComparisonService {

    /**
     * Compare all fields in the request and return a structured report.
     *
     * @param request contains documentType, userEnteredData, ocrExtractedData
     * @return full per-field comparison report with overall score and status
     */
    OcrComparisonReport compareFields(OcrComparisonRequest request);
}
