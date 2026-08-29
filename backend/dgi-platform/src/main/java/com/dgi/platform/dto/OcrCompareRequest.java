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
public class OcrCompareRequest {

    @NotBlank(message = "Document path or URL is required")
    private String documentPath;

    @NotBlank(message = "Document type is required")
    private String documentType; // CIN, PASSPORT, PATENTE

    private String expectedCin;
    private String expectedFirstName;
    private String expectedLastName;
    private String expectedBirthDate;
    private String expectedAddress;
}
