package com.piggy.piggybackend.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatbotResponse {
    private String answer;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NavigationLink> navigationLinks;

    private List<SuggestedQuestion> suggestedQuestions;

    // 기존 생성자 호환 (travel 응답 등)
    public ChatbotResponse(String answer, List<SuggestedQuestion> suggestedQuestions) {
        this.answer = answer;
        this.navigationLinks = null;
        this.suggestedQuestions = suggestedQuestions;
    }

    @Getter
    @AllArgsConstructor
    public static class SuggestedQuestion {
        private String question;
        private String preview;
    }

    @Getter
    @AllArgsConstructor
    public static class NavigationLink {
        private String label;  // 버튼에 표시될 텍스트
        private String route;  // 프론트 라우트 경로
    }
}