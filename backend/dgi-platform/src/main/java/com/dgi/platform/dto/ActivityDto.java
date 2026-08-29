package com.dgi.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDto {

    private Long id;

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String businessType;
    private String sector;
    private String address;
    private LocalDate startDate;
}
