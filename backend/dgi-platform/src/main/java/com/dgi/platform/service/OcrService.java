package com.dgi.platform.service;

import com.dgi.platform.dto.OcrCompareRequest;
import com.dgi.platform.dto.OcrVerificationResponse;

public interface OcrService {
    OcrVerificationResponse analyzeDocument(String documentPath, String documentType);
    OcrVerificationResponse compareOcrWithUserData(OcrCompareRequest request);
}
