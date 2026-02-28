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
    private NaverShopDto naverResults;    // 네이버 쇼핑 결과
    private RakutenItemDto rakutenResults; // 라쿠텐 쇼핑 결과
    private double exchangeRate;           // 적용된 실시간 환율 (JPY 100 기준)
}
