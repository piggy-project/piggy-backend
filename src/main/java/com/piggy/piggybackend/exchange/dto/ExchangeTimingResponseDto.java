package com.piggy.piggybackend.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExchangeTimingResponseDto {
    private String currencyCode;
    private int percentile;
}
