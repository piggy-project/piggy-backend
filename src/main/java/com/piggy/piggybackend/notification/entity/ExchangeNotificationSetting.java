package com.piggy.piggybackend.notification.entity;

import com.piggy.piggybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exchange_notification_settings")
public class ExchangeNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String currencyCode; // USD, JPY 등

    @Column(nullable = false)
    private Double targetPercent; // 1.0, 3.0, 5.0 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferencePeriod period; // DAILY, WEEKLY, MONTHLY

    @Column(nullable = false)
    private boolean isActive; // 알림 활성 여부

    private LocalDateTime lastNotifiedAt; // 도배 방지를 위한 마지막 발송 시각

    // 알림 발송 시 시각 업데이트
    public void updateLastNotifiedAt() {
        this.lastNotifiedAt = LocalDateTime.now();
    }

    // 알림 설정 변경 (수정용)
    public void updateSetting(Double targetPercent, ReferencePeriod period, boolean isActive) {
        this.targetPercent = targetPercent;
        this.period = period;
        this.isActive = isActive;
    }
}