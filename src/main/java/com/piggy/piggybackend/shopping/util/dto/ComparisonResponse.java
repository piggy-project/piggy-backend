package com.piggy.piggybackend.shopping.util.dto;

import java.util.List;
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
    private List<SerpItemDto> serpResults;
    private double exchangeRate;
    private String currencyCode;
}