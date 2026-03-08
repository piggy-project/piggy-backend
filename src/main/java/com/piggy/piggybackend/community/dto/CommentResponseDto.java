package com.piggy.piggybackend.community.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private String authorNickname;
    private LocalDateTime createdAt;

    @JsonProperty("isAuthor")
    private boolean isAuthor;
}
