package com.piggy.piggybackend.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferencePeriod {
    DAILY(1, "전일"),
    WEEKLY(7, "일주일 전"),
    MONTHLY(30, "한 달 전");

    private final int days;
    private final String label;
}