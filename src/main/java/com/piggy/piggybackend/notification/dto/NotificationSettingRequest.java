package com.piggy.piggybackend.notification.dto;

import com.piggy.piggybackend.notification.entity.ReferencePeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingRequest {

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "알림 기준 퍼센트는 필수입니다.")
    @Positive(message = "알림 기준 퍼센트는 0보다 커야 합니다.")
    private Double targetPercent;

    @NotNull(message = "기준 기간은 필수입니다.")
    private ReferencePeriod period;

    private boolean isActive;
}