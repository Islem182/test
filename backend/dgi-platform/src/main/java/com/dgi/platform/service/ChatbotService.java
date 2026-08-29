package com.dgi.platform.service;

import com.dgi.platform.dto.ChatQueryRequest;
import com.dgi.platform.dto.ChatQueryResponse;

import java.util.List;

public interface ChatbotService {
    ChatQueryResponse processQuery(String username, ChatQueryRequest request);
    List<ChatQueryResponse> getChatHistory(String username);
}
