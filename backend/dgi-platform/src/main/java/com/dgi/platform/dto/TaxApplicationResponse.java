package com.dgi.platform.dto;

import com.dgi.platform.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxApplicationResponse {
    private Long id;
    private Long userId;
    private String taxpayerName;
    private ApplicationStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private List<ActivityDto> activities;
    private List<DocumentDto> documents;
}
