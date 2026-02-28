package com.piggy.piggybackend.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeHistoryResponseDto {
    private String date;  // "MM-dd"
    private Double rate;  // 종가(환율)
}