package com.piggy.piggybackend.community.service;

import com.piggy.piggybackend.community.dto.PostCreateRequestDto;
import com.piggy.piggybackend.community.dto.PostLikeResponseDto;
import com.piggy.piggybackend.community.dto.PostResponseDto;
import com.piggy.piggybackend.community.entity.Post;
import com.piggy.piggybackend.community.entity.PostLike;
import com.piggy.piggybackend.community.repository.PostLikeRepository;
import com.piggy.piggybackend.community.repository.PostRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String LIKE_COUNT_KEY = "post:like:count:";
    private static final String VIEW_RECORD_KEY = "post:view:";

    // 1. 글 저장 (기존과 동일)
    @Transactional
    public PostResponseDto createPost(PostCreateRequestDto dto, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .user(user)
                .build();

        Post savedPost = postRepository.save(post);
        return convertToDto(savedPost);
    }

    // 2. 목록 조회 (Redis 데이터 반영)
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPosts(String category, String search, User user) {
        List<Post> posts = (search == null || search.isBlank())
                ? postRepository.findAllByCategoryOrderByCreatedAtDesc(category)
                : postRepository.searchByCategoryAndKeyword(category, search);

        return posts.stream().map(post -> {
            PostResponseDto dto = convertToDto(post);
            if (user != null) {
                boolean liked = postLikeRepository.findByPostAndUser(post, user).isPresent();
                dto.setLiked(liked);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // 3. 상세 조회 (Redis 데이터 반영)
    @Transactional
    public PostResponseDto getPostDetail(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 조회수 증가 로직 (로그인 유저 중복 방지 포함)
        if (user != null) {
            handleViewCount(postId, user.getId(), post);
        }

        // DTO 변환
        PostResponseDto dto = convertToDto(post);

        // 상세 조회 시에도 좋아요 여부를 체크해서 넣어줍니다.
        if (user != null) {
            boolean isLiked = postLikeRepository.findByPostAndUser(post, user).isPresent();
            dto.setLiked(isLiked); // DTO에 setLiked 메서드가 있어야 합니다.
        }

        return dto;
    }
    private void handleViewCount(Long postId, Long userId, Post post) {
        String viewKey = VIEW_RECORD_KEY + postId + ":" + userId;

        // 1. Redis에 해당 유저의 조회 기록이 있는지 확인
        if (Boolean.FALSE.equals(redisTemplate.hasKey(viewKey))) {
            // 2. 기록이 없다면 조회수 증가
            post.incrementViewCount();

            // 3. Redis에 기록 저장 (24시간 동안 유지)
            // 24시간이 지나면 다시 조회수가 올라갈 수 있게 설정 (보통의 커뮤니티 방식)
            redisTemplate.opsForValue().set(viewKey, "v", java.time.Duration.ofDays(1));
        }
    }

    // 4. 글 삭제
    @Transactional
    public void deletePost(Long postId, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        // 삭제 시 Redis 키도 함께 삭제
        redisTemplate.delete(LIKE_COUNT_KEY + postId);
        postRepository.delete(post);
    }

    // 5. 좋아요 토글 (DB와 Redis 동시 업데이트)
    @Transactional
    public PostLikeResponseDto toggleLike(Long postId, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);
        String redisKey = LIKE_COUNT_KEY + postId;

        boolean isLiked;
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            decrementRedisCount(redisKey, post.getLikeCount());
            isLiked = false;
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            post.incrementLikeCount();
            incrementRedisCount(redisKey, post.getLikeCount());
            isLiked = true;
        }

        return new PostLikeResponseDto(isLiked, getLikeCount(postId, post.getLikeCount()));
    }

    // --- Redis 헬퍼 메서드 ---

    private void incrementRedisCount(String key, Long dbCount) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount), java.time.Duration.ofDays(1));
        } else {
            redisTemplate.opsForValue().increment(key);
        }
    }

    private void decrementRedisCount(String key, Long dbCount) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount), java.time.Duration.ofDays(1));
        } else {
            String currentVal = redisTemplate.opsForValue().get(key);
            if (currentVal != null && Long.parseLong(currentVal) > 0) {
                redisTemplate.opsForValue().decrement(key);
            }
        }
    }

    private Long getLikeCount(Long postId, Long dbCount) {
        String key = LIKE_COUNT_KEY + postId;
        String val = redisTemplate.opsForValue().get(key);

        if (val == null) {
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount), java.time.Duration.ofHours(12));
            return dbCount;
        }
        return Long.parseLong(val);
    }

    private PostResponseDto convertToDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .viewCount(post.getViewCount())
                .likeCount(getLikeCount(post.getId(), post.getLikeCount()))
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}