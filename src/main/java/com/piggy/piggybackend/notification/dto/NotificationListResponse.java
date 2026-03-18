package com.piggy.piggybackend.notification.dto;

import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.notification.entity.ReferencePeriod;
import lombok.Getter;

@Getter
public class NotificationListResponse {
    private Long id;
    private String currencyCode;    // USD, JPY 등
    private Double targetPercent;   // 설정한 %
    private ReferencePeriod period; // DAILY, WEEKLY, MONTHLY
    private String periodLabel;     // "전일", "일주일 전" 등 (프론트 표시용)
    private boolean isActive;       // 활성화 여부

    public NotificationListResponse(ExchangeNotificationSetting setting) {
        this.id = setting.getId();
        this.currencyCode = setting.getCurrencyCode();
        this.targetPercent = setting.getTargetPercent();
        this.period = setting.getPeriod();
        this.periodLabel = setting.getPeriod().getLabel();
        this.isActive = setting.isActive();
    }
}