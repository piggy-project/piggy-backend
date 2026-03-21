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
            @RequestParam(required = false, defaultValue = "") String search,
            @LoginUser User user) {
        return ResponseEntity.ok(postService.getPosts(category, search, user));
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

    // 6. 실시간 핫 게시물(1위) 조회
    @GetMapping("/hot")
    public ResponseEntity<PostResponseDto> getTopHotPost(
            @LoginUser User user) { // 현재 로그인 유저 정보 (좋아요 여부 확인용)

        PostResponseDto hotPost = postService.getTopHotPost(user);

        // 핫 게시물이 존재하고 로그인한 유저라면, 좋아요/본인글 여부 추가 세팅
        if (hotPost != null && user != null) {
            enrichPostDto(hotPost, user);
        }

        return ResponseEntity.ok(hotPost);
    }

    // DTO에 현재 로그인한 사용자의 상태(좋아요 여부, 본인 글 여부)를 입혀주는 메서드
    private void enrichPostDto(PostResponseDto dto, User user) {
        if (user != null && dto != null) {
            // 1. 좋아요 여부 확인 (서비스의 public 메서드 호출)
            dto.setLiked(postService.isUserLiked(dto.getId(), user.getId()));

            // 2. 본인 글 여부 확인 (작성자 닉네임이나 ID로 비교)
            // DTO의 닉네임과 현재 로그인 유저의 닉네임이 같으면 본인 글임
            dto.setAuthor(dto.getAuthorNickname().equals(user.getNickname()));
        }
    }
}
