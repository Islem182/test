package com.dgi.platform.controller;

import com.dgi.platform.dto.ApiResponse;
import com.dgi.platform.dto.ChatQueryRequest;
import com.dgi.platform.dto.ChatQueryResponse;
import com.dgi.platform.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "AI Tax Assistant", description = "Chatbot support for registration procedures and FAQs")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/query")
    @Operation(summary = "Ask question to AI Tax Assistant")
    public ResponseEntity<ApiResponse<ChatQueryResponse>> query(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatQueryRequest request) {

        ChatQueryResponse response = chatbotService.processQuery(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("AI response generated", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get user chatbot conversation history")
    public ResponseEntity<ApiResponse<List<ChatQueryResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ChatQueryResponse> history = chatbotService.getChatHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved", history));
    }
}
