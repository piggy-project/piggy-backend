package com.piggy.piggybackend.community.service;

import com.piggy.piggybackend.community.dto.PostCreateRequestDto;
import com.piggy.piggybackend.community.dto.PostResponseDto;
import com.piggy.piggybackend.community.entity.Post;
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
public class PostService {

    private final PostRepository postRepository;

    // 1. 글 저장
    @Transactional
    public PostResponseDto createPost(PostCreateRequestDto dto, User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .user(user)
                .build();

        Post savedPost = postRepository.save(post);
        return convertToDto(savedPost);
    }

    // 2. 목록 조회 (검색 포함)
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPosts(String category, String search) {
        List<Post> posts;
        if (search == null || search.isBlank()) {
            posts = postRepository.findAllByCategoryOrderByCreatedAtDesc(category);
        } else {
            posts = postRepository.searchByCategoryAndKeyword(category, search);
        }
        return posts.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // 3. 상세 조회 (조회수 증가 포함)
    @Transactional
    public PostResponseDto getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.incrementViewCount(); // DB 직행 조회수 증가 (나중에 Redis로 바꿀 부분)
        return convertToDto(post);
    }

    // 4. 글 삭제
    @Transactional
    public void deletePost(Long postId, User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        postRepository.delete(post);
    }

    // 엔티티 -> DTO 변환 헬퍼
    private PostResponseDto convertToDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
