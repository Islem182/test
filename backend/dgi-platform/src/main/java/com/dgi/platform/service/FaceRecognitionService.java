package com.dgi.platform.service;

import com.dgi.platform.dto.FaceVerificationRequest;
import com.dgi.platform.dto.FaceVerificationResponse;

public interface FaceRecognitionService {
    FaceVerificationResponse verifyFace(FaceVerificationRequest request);
}
