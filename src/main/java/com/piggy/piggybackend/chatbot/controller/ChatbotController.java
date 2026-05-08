package com.piggy.piggybackend.chatbot.controller;

import com.piggy.piggybackend.chatbot.dto.ChatbotResponse;
import com.piggy.piggybackend.chatbot.dto.SuggestedOnlyResponse;
import com.piggy.piggybackend.chatbot.service.ChatbotService;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // 일반 질문
    @GetMapping("/ask")
    public ResponseEntity<ChatbotResponse> ask(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "질문을 입력해주세요.");
        }
        return ResponseEntity.ok(chatbotService.getChatResponse(question));
    }

    // 후속질문 클릭 시 - preview는 이미 있으니 suggestedQuestions만 반환
    @GetMapping("/suggested")
    public ResponseEntity<SuggestedOnlyResponse> suggested(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "질문을 입력해주세요.");
        }
        return ResponseEntity.ok(chatbotService.getSuggestedOnly(question));
    }
}