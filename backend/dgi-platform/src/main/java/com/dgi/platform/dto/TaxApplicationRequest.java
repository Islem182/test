package com.dgi.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxApplicationRequest {

    @NotEmpty(message = "At least one activity is required")
    @Valid
    private List<ActivityDto> activities;

    @Valid
    private List<DocumentDto> documents;
}
