package com.piggy.piggybackend.notification.dto;

import com.piggy.piggybackend.notification.entity.ReferencePeriod;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingRequest {
    private String currencyCode;    // 예: "USD"
    private Double targetPercent;   // 예: 1.0, 3.0, 5.0
    private ReferencePeriod period; // DAILY, WEEKLY, MONTHLY
    private boolean isActive;       // 알림 켬/끎
}