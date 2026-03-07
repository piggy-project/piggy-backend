package com.piggy.piggybackend.exchange.dto;

import com.piggy.piggybackend.exchange.constant.BankPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExchangeSimulateRequest {
    private String targetCurrency; // 목표 통화 (예: "USD")
    private Long krwAmount;        // 환전할 원화 (예: 1000000)
    private BankPolicy bankPolicy; // 선택한 은행 (KB, SHINHAN, TOSS 등)

    // CUSTOM(직접입력) 선택 시 사용할 값
    private Double customFeeRate;
    private Double customDiscountRate;
}