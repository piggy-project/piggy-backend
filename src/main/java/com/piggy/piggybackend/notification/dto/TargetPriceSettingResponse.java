package com.piggy.piggybackend.notification.dto;

import com.piggy.piggybackend.notification.entity.ExchangeTargetPriceSetting;
import com.piggy.piggybackend.notification.entity.PriceDirection;
import lombok.Getter;

@Getter
public class TargetPriceSettingResponse {
    private Long id;
    private String currencyCode;
    private Double targetPrice;
    private PriceDirection direction;
    private String directionLabel; // "이하", "이상"
    private boolean isActive;

    public TargetPriceSettingResponse(ExchangeTargetPriceSetting setting) {
        this.id = setting.getId();
        this.currencyCode = setting.getCurrencyCode();
        this.targetPrice = setting.getTargetPrice();
        this.direction = setting.getDirection();
        this.directionLabel = setting.getDirection().getLabel();
        this.isActive = setting.isActive();
    }
}