package com.dgi.platform.dto;

import com.dgi.platform.entity.DocumentType;
import com.dgi.platform.entity.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDto {

    private Long id;

    @NotNull(message = "Document type is required")
    private DocumentType type;

    @NotBlank(message = "File path is required")
    private String filePath;

    private LocalDateTime uploadDate;
    private VerificationStatus verificationStatus;
}
