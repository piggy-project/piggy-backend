package com.piggy.piggybackend.shopping.util.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
    private String originalText;   // 인식된 텍스트 (예: 500円, $10.99)
    private String currencyCode;   // 통화 코드 (예: USD)
    private double foreignAmount;  // 외화 금액
    private long krwAmount;       // 계산된 원화 금액
    private double exchangeRate;   // 적용된 환율
}