package com.dgi.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQueryResponse {

    private Long id;
    private String question;
    private String answer;
    private LocalDateTime date;
}
