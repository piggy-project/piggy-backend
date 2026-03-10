package com.piggy.piggybackend.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SuggestedOnlyResponse {
    private List<ChatbotResponse.SuggestedQuestion> suggestedQuestions;
}