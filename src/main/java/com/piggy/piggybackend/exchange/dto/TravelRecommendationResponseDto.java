package com.piggy.piggybackend.exchange.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TravelRecommendationResponseDto {

    private String destination;   // 여행지명 (예: 일본 도쿄)
    private String reason;        // 추천 이유
    private String currencyCode;  // 통화 코드 (JPY, THB ...)
    private String currencyName;  // 통화명 (일본 엔 ...)
    private double exchangeRate;  // 현재 환율 (원화 기준)
    private double percentChange; // 전일 대비 등락률 (%)
}
