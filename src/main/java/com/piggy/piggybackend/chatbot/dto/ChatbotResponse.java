package com.piggy.piggybackend.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String answer;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NavigationLink> navigationLinks;

    private List<SuggestedQuestion> suggestedQuestions;

    public ChatbotResponse(String answer, List<SuggestedQuestion> suggestedQuestions) {
        this.answer = answer;
        this.navigationLinks = null;
        this.suggestedQuestions = suggestedQuestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedQuestion {
        private String question;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavigationLink {
        private String label;
        private String route;
    }
}