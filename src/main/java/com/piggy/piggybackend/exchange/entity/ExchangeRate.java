package com.piggy.piggybackend.exchange.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String curUnit;   // 통화 코드 (예: USD)
    private String curNm;     // 통화명
    private Double dealBasR;  // 매매 기준율
    private String searchDate; // 조회 날짜 (YYYYMMDD)

    @Builder
    public ExchangeRate(String curUnit, String curNm, Double dealBasR, String searchDate) {
        this.curUnit = curUnit;
        this.curNm = curNm;
        this.dealBasR = dealBasR;
        this.searchDate = searchDate;
    }
}
