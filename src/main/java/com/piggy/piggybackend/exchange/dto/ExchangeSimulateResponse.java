package com.piggy.piggybackend.exchange.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExchangeSimulateResponse {
    private String currency;          // 통화 코드
    private Double baseRate;          // 매매기준율 (예: 1350.5)
    private Double appliedRate;       // 우대 수수료가 적용된 최종 환율

    private Long inputKrwAmount;      // 입력한 원화
    private Double finalForeignAmount;// 최종 수령 외화

    private Long originalFeeKrw;      // 우대 전 원래 냈어야 할 수수료(원)
    private Long savedFeeKrw;         // 우대받아서 아낀 수수료(원) - 이득 금액!
}