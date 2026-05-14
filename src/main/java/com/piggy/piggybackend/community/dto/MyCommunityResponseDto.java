package com.piggy.piggybackend.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyCommunityResponseDto {
    private List<PostResponseDto> myPosts;
    private List<MyCommentResponseDto> myComments;
    private List<PostResponseDto> likedPosts;
}
