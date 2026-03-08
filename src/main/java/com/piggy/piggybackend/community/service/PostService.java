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
import org.springframework.scheduling.annotation.Scheduled;
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
    private static final String LIKE_USERS_KEY = "post:like:users:";
    private static final String VIEW_RECORD_KEY = "post:view:record:";
    private static final String VIEW_COUNT_KEY = "post:view:count:";

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
                dto.setLiked(isUserLiked(post.getId(), user.getId()));
                dto.setAuthor(post.getUser().getId().equals(user.getId()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // 3. 상세 조회 (Redis 데이터 반영)
    @Transactional
    public PostResponseDto getPostDetail(Long postId, User user) {
        System.out.println(user);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 조회수 증가 로직 (로그인 유저 중복 방지 포함)
        if (user != null) {
            handleViewCount(postId, user.getId(), post);
        }

        // DTO 변환
        PostResponseDto dto = convertToDto(post);

        // 상세 조회 시 redis 확인
        if (user != null) {
            dto.setLiked(isUserLiked(postId, user.getId()));
            dto.setAuthor(post.getUser().getId().equals(user.getId()));
        }

        return dto;
    }
    private void handleViewCount(Long postId, Long userId, Post post) {
        String recordKey = VIEW_RECORD_KEY + postId + ":" + userId;
        String countKey = VIEW_COUNT_KEY + postId;

        // 중복 조회 확인
        if (!redisTemplate.hasKey(recordKey)) {
            // 처음 보는 유저라면 Redis의 전체 조회수 키를 1 증가시킴 (DB는 건드리지 않음!)
            if (!redisTemplate.hasKey(countKey)) {
                // Redis에 키가 없으면 현재 DB 값을 가져와서 초기화 후 증가
                redisTemplate.opsForValue().set(countKey, String.valueOf(post.getViewCount() + 1), java.time.Duration.ofDays(1));
            } else {
                redisTemplate.opsForValue().increment(countKey);
            }

            // 중복 방지 기록 저장 (24시간)
            redisTemplate.opsForValue().set(recordKey, "v", java.time.Duration.ofDays(1));
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
        redisTemplate.delete(LIKE_USERS_KEY + postId);
        redisTemplate.delete(VIEW_COUNT_KEY + postId);

        postRepository.delete(post);
    }

    // 5. 좋아요 토글 (DB 부하 경감 방식)
    @Transactional
    public PostLikeResponseDto toggleLike(Long postId, User user) {
        if (user == null) throw new CustomException(ErrorCode.LOGIN_REQUIRED);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        String userSetKey = LIKE_USERS_KEY + postId;
        String countKey = LIKE_COUNT_KEY + postId;

        Long result = redisTemplate.opsForSet().add(userSetKey, user.getId().toString());

        boolean isLiked;
        if (result != null && result > 0) {
            // 1이 반환되면 새로 추가된 것 -> 좋아요
            redisTemplate.opsForValue().increment(countKey);
            isLiked = true;
        } else {
            // 0이 반환되면 이미 있었던 것 -> 좋아요 취소
            redisTemplate.opsForSet().remove(userSetKey, user.getId().toString());
            redisTemplate.opsForValue().decrement(countKey);
            isLiked = false;
        }
        return new PostLikeResponseDto(isLiked, getLikeCount(postId, post.getLikeCount()));
    }
    // 정각마다 조회수 & 좋아요 수를 DB에 일괄 업데이트
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void syncRedisToDb() {
        // 1. 조회수 동기화
        syncCounts(VIEW_COUNT_KEY, (post, count) -> post.setViewCount(count));
        // 2. 좋아요 수 동기화
        syncCounts(LIKE_COUNT_KEY, (post, count) -> post.setLikeCount(count));
    }

    // 공통 동기화 로직
    private void syncCounts(String keyPrefix, java.util.function.BiConsumer<Post, Long> setter) {
        //나중에 게시글이 수만개가 넘어가면 redis가 멈출 수 있음. 그럼 그때는 SCAN으로 바꿔야함
        java.util.Set<String> keys = redisTemplate.keys(keyPrefix + "*");
        if (keys == null) return;

        for (String key : keys) {
            Long postId = Long.parseLong(key.replace(keyPrefix, ""));
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr != null) {
                postRepository.findById(postId).ifPresent(post -> {
                    setter.accept(post, Long.parseLong(countStr));
                });
            }
        }
    }

    // --- Redis 헬퍼 메서드 ---

    private boolean isUserLiked(Long postId, Long userId) {
        // Redis Set에 해당 유저가 있는지 확인
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(LIKE_USERS_KEY + postId, userId.toString()));
    }
    private Long getLikeCount(Long postId, Long dbCount) {
        String key = LIKE_COUNT_KEY + postId;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            // Redis에 없으면 DB 값으로 초기화 (이후에는 Redis가 기준이 됨)
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount), java.time.Duration.ofDays(1));
            return dbCount;
        }
        return Long.parseLong(val);
    }
    private Long getViewCount(Long postId, Long dbCount) {
        String key = VIEW_COUNT_KEY + postId;
        String val = redisTemplate.opsForValue().get(key);

        if (val == null) {
            // Redis에 없으면 DB 값을 기준으로 세팅 (12시간 유지)
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
                .viewCount(getViewCount(post.getId(), post.getViewCount()))
                .likeCount(getLikeCount(post.getId(), post.getLikeCount()))
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}