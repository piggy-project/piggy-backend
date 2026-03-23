package com.piggy.piggybackend.exchange.entity;

import com.piggy.piggybackend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exchange_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency_code"}))
public class ExchangeFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "is_lowest_alert_on", columnDefinition = "boolean default false")
    private boolean isLowestAlertOn;    // 최저가 알림 설정 여부

    @Column(name = "is_volatility_alert_on", columnDefinition = "boolean default false")
    private boolean isVolatilityAlertOn; // 급변동 알림 설정 여부

    public void toggleLowestAlert(boolean status) { this.isLowestAlertOn = status; }
    public void toggleVolatilityAlert(boolean status) { this.isVolatilityAlertOn = status; }
}