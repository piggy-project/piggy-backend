package com.piggy.piggybackend.exchange.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankPolicy {
    // 은행명(기본 수수료율 %, 최대 우대율 %)
    KB("국민은행", 1.75, 90.0),
    SHINHAN("신한은행", 1.75, 90.0),
    IBK("기업은행", 1.75, 90.0),
    TOSS("토스뱅크", 1.75, 100.0), // 토스는 살때/팔때 100% 우대 (수수료 무료)
    CUSTOM("직접입력", 0.0, 0.0);

    private final String bankName;
    private final double defaultFeeRate; // 기본 수수료율 (예: 1.75%)
    private final double maxDiscountRate; // 최대 우대율 (예: 90.0%)
}