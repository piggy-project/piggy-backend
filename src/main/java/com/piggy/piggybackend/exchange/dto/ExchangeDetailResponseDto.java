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
    private Integer timing;       // 최근 90일 대비 백분위 (환율 저렴도)
}
