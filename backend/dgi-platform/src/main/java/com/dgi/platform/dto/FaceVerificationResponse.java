package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceVerificationResponse {

    private boolean identityVerified;
    private double similarityScore;
    private boolean livenessVerified;
    private String statusMessage;
}
