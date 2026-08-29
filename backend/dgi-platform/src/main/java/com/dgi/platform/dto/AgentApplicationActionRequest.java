package com.dgi.platform.dto;

import com.dgi.platform.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentApplicationActionRequest {

    @NotNull(message = "Status action is required (APPROVED or REJECTED)")
    private ApplicationStatus status;

    private String comments;
}
