package com.piggy.piggybackend.community.controller;

import com.piggy.piggybackend.community.dto.CommentRequestDto;
import com.piggy.piggybackend.community.dto.CommentResponseDto;
import com.piggy.piggybackend.community.service.CommentService;
import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDto dto,
            @LoginUser User user) {
        return ResponseEntity.ok(commentService.createComment(postId, dto, user));
    }

    // 댓글 목록 조회
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long postId,
            @LoginUser User user) {
        return ResponseEntity.ok(commentService.getComments(postId, user));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @LoginUser User user) {
        commentService.deleteComment(commentId, user);
        return ResponseEntity.noContent().build();
    }
}
