package com.dgi.platform.service;

import com.dgi.platform.dto.OcrVerificationReport;
import com.dgi.platform.dto.OcrVerifyDocumentsRequest;

/**
 * Builds a structured per-document + cross-document verification report
 * from the three OCR extraction results produced in Step 3.
 *
 * No file I/O — operates entirely on the already-extracted text maps.
 */
public interface OcrVerificationService {

    /**
     * Verify all three documents and produce a structured report.
     *
     * @param request contains cinData, passportData, certificatData
     *                (the extractedData maps from OcrVerificationResponse)
     * @return full OcrVerificationReport with per-document sections and
     *         cross-document identity consistency
     */
    OcrVerificationReport buildReport(OcrVerifyDocumentsRequest request);
}
