package com.dgi.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceVerificationRequest {

    @NotBlank(message = "Document photo URL/Base64 is required")
    private String documentPhoto;

    @NotBlank(message = "Live webcam photo Base64 is required")
    private String liveSelfiePhoto;
}
