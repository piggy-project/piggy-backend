package com.piggy.piggybackend.user.entity;

import com.piggy.piggybackend.community.entity.Comment;
import com.piggy.piggybackend.community.entity.Post;
import com.piggy.piggybackend.community.entity.PostLike;
import com.piggy.piggybackend.shopping.util.entity.ShoppingFavorite;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"}),
                @UniqueConstraint(columnNames = "nickname")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String email; // 소셜은 null 가능

    @Column(length = 128)
    private String password; // LOCAL만 사용

    @Column(length = 20)
    private String name;

    @Column(length = 12)  // DB 제약조건: 12자 이내
    private String nickname;

    @Column
    private LocalDate birth;

    @Column(length = 256)
    private String phoneNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = true)
    private String providerId;

    // 내가 쓴 게시글들
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    // 내가 쓴 댓글들
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 내가 누른 좋아요
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ShoppingFavorite> shoppingFavorites = new ArrayList<>();

    @Column(length = 512)
    private String profileImageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<FcmToken> fcmTokens = new ArrayList<>();

    // 소프트 삭제 메서드
    public void softDelete() {
        if (this.deletedAt != null) {
            throw new IllegalStateException("이미 탈퇴 처리된 회원입니다.");
        }
        this.deletedAt = LocalDateTime.now();
    }

    // 계정 복구 메서드
    public void recover() {
        this.deletedAt = null;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateSocialInfo(String nickname, LocalDate birth, String phoneNum) {
        this.nickname = nickname;
        this.birth = birth;
        this.phoneNum = phoneNum;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
