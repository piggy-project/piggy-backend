package com.piggy.piggybackend.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piggy.piggybackend.notification.entity.PriceDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TargetPriceSettingRequest {

    @NotBlank(message = "통화 코드는 필수입니다.")
    private String currencyCode;

    @NotNull(message = "목표 환율은 필수입니다.")
    @Positive(message = "목표 환율은 0보다 커야 합니다.")
    private Double targetPrice;

    @NotNull(message = "알림 방향(ABOVE/BELOW)은 필수입니다.")
    private PriceDirection direction;

    // Lombok getter(isActive())만 있으면 Jackson이 "active" 읽기 전용 속성으로 인식해 값이 항상 false가 됨
    @JsonProperty("isActive")
    @JsonAlias("active")
    private boolean isActive;
}