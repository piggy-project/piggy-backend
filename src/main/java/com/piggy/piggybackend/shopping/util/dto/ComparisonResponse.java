package com.piggy.piggybackend.shopping.util.dto;

import java.util.List; // 추가
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {
    private NaverShopDto naverResults;
    private RakutenItemDto rakutenResults;
    private List<SerpItemDto> serpResults; // List로 유지
    private double exchangeRate;
    private String currencyCode;
}