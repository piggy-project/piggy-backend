package com.piggy.piggybackend.user.entity;

import com.piggy.piggybackend.global.util.FcmTokenConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = FcmTokenConverter.class)
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}