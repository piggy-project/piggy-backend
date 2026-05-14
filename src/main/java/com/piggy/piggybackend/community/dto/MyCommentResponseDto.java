package com.piggy.piggybackend.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MyCommentResponseDto {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long postId;
    private String postTitle;
}
