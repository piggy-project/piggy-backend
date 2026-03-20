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
@Table(name = "exchange_target_price_settings")
public class ExchangeTargetPriceSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String currencyCode;     // USD, JPY 등

    @Column(nullable = false)
    private Double targetPrice;      // 목표 환율 (예: 950.0)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceDirection direction; // ABOVE or BELOW

    @Column(nullable = false)
    private boolean isActive;

    private LocalDateTime lastNotifiedAt;

    public void updateLastNotifiedAt() {
        this.lastNotifiedAt = LocalDateTime.now();
    }

    public void updateSetting(Double targetPrice, PriceDirection direction, boolean isActive) {
        this.targetPrice = targetPrice;
        this.direction = direction;
        this.isActive = isActive;
    }
}