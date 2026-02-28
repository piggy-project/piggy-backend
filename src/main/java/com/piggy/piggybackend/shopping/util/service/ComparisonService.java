package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import com.piggy.piggybackend.shopping.util.dto.ComparisonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final NaverShopService naverService;
    private final RakutenService rakutenService;
    private final ExchangeRateService exchangeRateService; // 환율 서비스 주입

    public ComparisonResponse comparePrices(String query) {
        try {
            // 1. 각 쇼핑몰 API 데이터 호출
            var naverResult = naverService.searchItems(query);
            var rakutenResult = rakutenService.searchItems(query);

            // 2. Redis에서 실시간 JPY 환율 가져오기
            var jpyDetail = exchangeRateService.getExchangeDetail("JPY");
            double exchangeRate = jpyDetail.getCurrentRate();

            // 3. 최종 응답 객체 생성
            return ComparisonResponse.builder()
                    .naverResults(naverResult)
                    .rakutenResults(rakutenResult)
                    .exchangeRate(exchangeRate)
                    .build();
        } catch (Exception e) {
            log.error("검색 중 에러 발생: ", e); // 에러 로그 추가
            throw e;
        }

    }
}