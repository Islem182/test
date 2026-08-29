package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.FaceVerificationRequest;
import com.dgi.platform.dto.FaceVerificationResponse;
import com.dgi.platform.service.FaceRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
@Tag(name = "Biometric Face Recognition", description = "Webcam selfie vs Document photo verification")
public class FaceRecognitionController {

    private final FaceRecognitionService faceRecognitionService;

    @PostMapping("/verify")
    @Operation(summary = "Compare live webcam selfie photo with document identity photo")
    public ResponseEntity<ApiResponse<FaceVerificationResponse>> verifyFace(
            @Valid @RequestBody FaceVerificationRequest request) {

        FaceVerificationResponse response = faceRecognitionService.verifyFace(request);
        return ResponseEntity.ok(ApiResponse.success(response.getStatusMessage(), response));
    }
}
