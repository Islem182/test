package com.dgi.platform.dto;

import com.dgi.platform.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {

    private Long id;
    private Long taxpayerId;
    private String taxpayerName;
    private String subject;
    private String message;
    private ComplaintStatus status;
    private String response;
    private LocalDateTime createdAt;
}
