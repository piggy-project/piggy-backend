package com.piggy.piggybackend.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikeResponseDto {
    private boolean isLiked;   // 현재 좋아요 상태 (true: 누름, false: 취소함)
    private Long likeCount;    // 변경된 후의 전체 좋아요 수
}