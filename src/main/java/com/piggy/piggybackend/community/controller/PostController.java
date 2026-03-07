package com.piggy.piggybackend.community.controller;

import com.piggy.piggybackend.community.dto.PostCreateRequestDto;
import com.piggy.piggybackend.community.dto.PostLikeResponseDto;
import com.piggy.piggybackend.community.dto.PostResponseDto;
import com.piggy.piggybackend.community.service.PostService;
import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class PostController {

    private final PostService postService;

    // 1. 게시글 생성
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestBody PostCreateRequestDto dto,
            @LoginUser User user) { // 인증된 현재 사용자 정보
        return ResponseEntity.ok(postService.createPost(dto, user));
    }

    // 2. 게시글 목록 조회
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getPosts(
            @RequestParam String category,
            @RequestParam(required = false, defaultValue = "") String search) {
        return ResponseEntity.ok(postService.getPosts(category, search));
    }

    // 3. 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPostDetail(
            @PathVariable Long postId,
            @LoginUser User user) {
        return ResponseEntity.ok(postService.getPostDetail(postId, user));
    }

    // 4. 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @LoginUser User user) {
        postService.deletePost(postId, user);
        return ResponseEntity.noContent().build();
    }
    // 5. 좋아요 토글
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponseDto> toggleLike(
            @PathVariable Long postId,
            @LoginUser User user) {
        return ResponseEntity.ok(postService.toggleLike(postId, user));
    }
}
