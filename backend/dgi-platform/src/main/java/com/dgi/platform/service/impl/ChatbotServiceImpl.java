package com.dgi.platform.service.impl;

import com.dgi.platform.dto.ChatQueryRequest;
import com.dgi.platform.dto.ChatQueryResponse;
import com.dgi.platform.entity.ChatHistory;
import com.dgi.platform.entity.User;
import com.dgi.platform.exception.ResourceNotFoundException;
import com.dgi.platform.repository.ChatHistoryRepository;
import com.dgi.platform.repository.UserRepository;
import com.dgi.platform.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatQueryResponse processQuery(String username, ChatQueryRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        String q = request.getQuestion().toLowerCase();
        String answer;

        if (q.contains("document") || q.contains("pièce") || q.contains("papier")) {
            answer = "Required registration documents include: 1) Certified copy of National Identity Card (CIN) or Passport, 2) Commercial Register (Registre de Commerce), 3) Lease Agreement or Property Title, and 4) Tax Declaration Form.";
        } else if (q.contains("procedure") || q.contains("étape") || q.contains("comment")) {
            answer = "To complete your tax registration: 1) Fill out your Personal & Business Info in the 'New Registration' wizard, 2) Upload your scanned CIN and Business documents, 3) Perform webcam biometric face verification, and 4) Submit for DGI Agent review.";
        } else if (q.contains("status") || q.contains("état") || q.contains("suivi")) {
            answer = "You can track your application status anytime under the 'My Applications' tab in your taxpayer portal. Once an agent approves your file, your official Fiscal Number will be generated automatically!";
        } else if (q.contains("contact") || q.contains("support") || q.contains("réclamation")) {
            answer = "You can file a formal complaint or inquiry via the 'Complaints' section of the platform. Our tax agents respond within 24-48 business hours.";
        } else {
            answer = "Thank you for contacting DGI Online Services Assistant. For tax registration, submit your file via the 'New Registration' wizard. For specific inquiries, select one of our quick topics or contact a tax agent.";
        }

        ChatHistory chatHistory = ChatHistory.builder()
                .user(user)
                .question(request.getQuestion())
                .answer(answer)
                .build();

        ChatHistory saved = chatHistoryRepository.save(chatHistory);

        return ChatQueryResponse.builder()
                .id(saved.getId())
                .question(saved.getQuestion())
                .answer(saved.getAnswer())
                .date(saved.getDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatQueryResponse> getChatHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return chatHistoryRepository.findByUserIdOrderByDateDesc(user.getId()).stream()
                .map(ch -> ChatQueryResponse.builder()
                        .id(ch.getId())
                        .question(ch.getQuestion())
                        .answer(ch.getAnswer())
                        .date(ch.getDate())
                        .build())
                .collect(Collectors.toList());
    }
}
