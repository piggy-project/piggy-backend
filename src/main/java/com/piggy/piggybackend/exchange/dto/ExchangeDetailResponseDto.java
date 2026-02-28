package com.piggy.piggybackend.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
//프론트 보낼 상세환율 dto
public class ExchangeDetailResponseDto {
    private String code;
    private Double currentRate;
    private Double high;
    private Double low;
    private Double change;
    private Double percentChange;
}
