package com.piggy.piggybackend.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 게시글 생성 요청 DTO
public class PostCreateRequestDto {
    private String title;
    private String content;
    private String category;
}
