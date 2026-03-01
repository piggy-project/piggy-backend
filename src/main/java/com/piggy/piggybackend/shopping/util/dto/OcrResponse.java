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
    private String originalText; // 인식된 텍스트 중 대표값 (예: 1,500円)
    private double jpyAmount;    // 추출된 엔화 숫자
    private long krwAmount;      // 실시간 환율이 적용된 원화 금액
}