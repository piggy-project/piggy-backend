package com.piggy.piggybackend.exchange.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExchangeFavoriteResponseDto {

    private String currencyCode;   // "JPY"
    private String currencyName;   // "일본 엔"
    private double currentRate;    // 현재 환율
    private double change;         // 전일 대비 변동액
    private double percentChange;  // 전일 대비 변동률(%)
    private double high;           // 당일 고가
    private double low;            // 당일 저가
}