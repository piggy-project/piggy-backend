package com.piggy.piggybackend.community.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("isLiked")
    private boolean isLiked;

    @JsonProperty("isAuthor")
    private boolean isAuthor;
}
