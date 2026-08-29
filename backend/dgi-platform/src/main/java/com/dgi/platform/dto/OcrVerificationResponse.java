package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrVerificationResponse {

    private String verification; // SUCCESS / FAILED
    private double confidenceScore;
    private String documentType;
    private Map<String, String> extractedData;
    private List<String> differences;
}
