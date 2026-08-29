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
public class ComplaintRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message content is required")
    private String message;
}
