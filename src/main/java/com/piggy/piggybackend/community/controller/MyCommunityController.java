package com.piggy.piggybackend.community.controller;

import com.piggy.piggybackend.community.dto.MyCommentResponseDto;
import com.piggy.piggybackend.community.dto.MyCommunityResponseDto;
import com.piggy.piggybackend.community.dto.PostResponseDto;
import com.piggy.piggybackend.community.service.CommentService;
import com.piggy.piggybackend.community.service.PostService;
import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/me")
public class MyCommunityController {

    private final PostService postService;
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<MyCommunityResponseDto> getMyCommunityActivities(@LoginUser User user) {
        return ResponseEntity.ok(MyCommunityResponseDto.builder()
                .myPosts(postService.getMyPosts(user))
                .myComments(commentService.getMyComments(user))
                .likedPosts(postService.getMyLikedPosts(user))
                .build());
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(@LoginUser User user) {
        return ResponseEntity.ok(postService.getMyPosts(user));
    }

    @GetMapping("/comments")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(@LoginUser User user) {
        return ResponseEntity.ok(commentService.getMyComments(user));
    }

    @GetMapping("/liked-posts")
    public ResponseEntity<List<PostResponseDto>> getMyLikedPosts(@LoginUser User user) {
        return ResponseEntity.ok(postService.getMyLikedPosts(user));
    }
}
