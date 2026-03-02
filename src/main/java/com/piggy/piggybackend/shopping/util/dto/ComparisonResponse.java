package com.piggy.piggybackend.shopping.util.dto;
import com.piggy.piggybackend.shopping.util.dto.NaverShopDto;
import com.piggy.piggybackend.shopping.util.dto.RakutenItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {
    private NaverShopDto naverResults;       // 한국 네이버 결과
    private RakutenItemDto rakutenResults;   // 일본 라쿠텐 결과 (JPY일 때)
    private SerpItemDto serpResults;       // 동남아 결과 (THB, PHP 등일 때)
    private double exchangeRate;             // 선택된 통화의 환율
    private String currencyCode;        // 적용된 실시간 환율 (JPY 100 기준)
}
