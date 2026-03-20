package com.piggy.piggybackend.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceDirection {
    BELOW("이하"),   // 목표가 이하로 떨어지면
    ABOVE("이상");   // 목표가 이상으로 오르면

    private final String label;
}