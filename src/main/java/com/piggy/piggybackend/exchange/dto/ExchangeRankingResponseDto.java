package com.piggy.piggybackend.exchange.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExchangeRankingResponseDto {

    private String curUnit;       // 통화 코드 (USD, JPY ...)
    private String curNm;         // 통화명 (미국 달러 ...)
    private double rate;          // 현재 환율
    private double change;        // 전일 대비 변동폭
    private double percentChange; // 전일 대비 등락률 (%)
}