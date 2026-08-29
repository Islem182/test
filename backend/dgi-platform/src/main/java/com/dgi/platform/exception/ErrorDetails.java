package com.dgi.platform.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private Map<String, String> validationErrors;
}
