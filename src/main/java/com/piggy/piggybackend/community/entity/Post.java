package com.piggy.piggybackend.community.entity;

import com.piggy.piggybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // 생성일/수정일 자동화
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id; // 게시판키값

    @Column(nullable = false, length = 200)
    private String title; // 게시판 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 게시판 내용

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L; // 조회수 (기본값 0)

    @Builder.Default
    private Long likeCount = 0L; // 좋아요 수 (기본값 0)

    @Column(nullable = false)
    private String category; // 게시판 종류 (환율 수다방, 여행 공유방 등)

    @Column(length = 10)
    private String code; // 통화 코드 (해당 글이 어떤 국가와 관련 있는지)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 변경일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 작성자 (사용자 아이디 연관관계)

    // --- 비즈니스 로직 ---

    // 조회수 증가
    public void incrementViewCount() {
        this.viewCount++;
    }

    // 게시글 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
    // 좋아요 증가
    public void incrementLikeCount() {
        this.likeCount++;
    }

    // 좋아요 감소 (0 이하로 내려가지 않게 방어)
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
