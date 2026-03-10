package com.piggy.piggybackend.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotResponse {
    private String answer;
    private List<SuggestedQuestion> suggestedQuestions;

    @Getter
    @AllArgsConstructor
    public static class SuggestedQuestion {
        private String question;
        private String preview; // 예상 답변 한 줄 요약
    }
}