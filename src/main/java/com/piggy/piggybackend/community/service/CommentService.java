package com.piggy.piggybackend.community.service;

import com.piggy.piggybackend.community.dto.CommentRequestDto;
import com.piggy.piggybackend.community.dto.CommentResponseDto;
import com.piggy.piggybackend.community.entity.Comment;
import com.piggy.piggybackend.community.entity.Post;
import com.piggy.piggybackend.community.repository.CommentRepository;
import com.piggy.piggybackend.community.repository.PostRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto dto, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .post(post)
                .user(user)
                .build();

        commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        post.incrementCommentCount();

        return convertToDto(comment, user);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return commentRepository.findByPostOrderByCreatedAtAsc(post)
                .stream()
                .map(comment -> convertToDto(comment, user))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 본인 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);

        // 게시글의 댓글 수 감소
        post.decrementCommentCount();
    }
    // DTO 변환 헬퍼
    private CommentResponseDto convertToDto(Comment comment, User user) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorNickname(comment.getUser().getNickname())
                .createdAt(comment.getCreatedAt())
                .isAuthor(user != null && comment.getUser().getId().equals(user.getId()))
                .build();
    }
}
