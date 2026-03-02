package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.redis.service.SearchRankingService;
import com.piggy.piggybackend.shopping.util.dto.ComparisonResponse;
import com.piggy.piggybackend.shopping.util.dto.RakutenItemDto;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import com.piggy.piggybackend.shopping.util.dto.UnifiedItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final NaverShopService naverService;
    private final RakutenService rakutenService;
    private final ExchangeRateService exchangeRateService;
    private final SearchRankingService rankingService;
    private final StringRedisTemplate redisTemplate; // Redis 연동을 위한 템플릿
    private final ObjectMapper objectMapper; // JSON 변환을 위한 객체
    private final SerpShopService serpShopService;

    private static final String CACHE_KEY_PREFIX = "popular_items_cache:";
    public ComparisonResponse comparePrices(String query, String currencyCode) {
        try {
            rankingService.incrementKeywordCount(query);

            // 1. 네이버 API 호출
            var naverResult = naverService.searchItems(query);

            // 2. 외화 결과 분기 처리
            RakutenItemDto rakutenResult = null;
            List<SerpItemDto> serpResults = null; // 변수명 복수형으로 통일

            if ("JPY".equals(currencyCode)) {
                rakutenResult = rakutenService.searchItems(query);
            }
            // 베트남(VND) 조건 추가
            else if ("THB".equals(currencyCode) || "PHP".equals(currencyCode) || "VND".equals(currencyCode)) {
                // 이전에 수정한 getRegionalProducts(리스트 반환 메서드) 호출
                serpResults = serpShopService.getRegionalProducts(query, currencyCode);
            }

            // 3. 환율 데이터 확인
            var exchangeDetail = exchangeRateService.getExchangeDetail(currencyCode);
            if (exchangeDetail == null) {
                throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
            }
            double exchangeRate = exchangeDetail.getCurrentRate();

            // 4. 결과 조립
            return ComparisonResponse.builder()
                    .naverResults(naverResult)
                    .rakutenResults(rakutenResult)
                    .serpResults(serpResults) // 명칭 일치
                    .exchangeRate(exchangeRate)
                    .currencyCode(currencyCode)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("검색 중 예상치 못한 에러 (통화: {}): ", currencyCode, e);
            throw new CustomException(ErrorCode.SHOPPING_API_ERROR);
        }
    }

    // 인기 아이템 리스트 (캐시 읽기)
    // 1. 통화별 추천 아이템 가져오기
    public List<UnifiedItemDto> getPopularItems(String currencyCode) {
        String cacheKey = CACHE_KEY_PREFIX + currencyCode;
        try {
            List<UnifiedItemDto> cached = getCachedItems(cacheKey);
            if (cached == null || cached.isEmpty()) {
                // 캐시가 없으면 즉시 한 번 갱신 시도 (혹은 빈 리스트 반환)
                return List.of();
            }
            return cached;
        } catch (Exception e) {
            log.error("추천 아이템 조회 에러: ", e);
            return List.of();
        }
    }

    // 인기 아이템(캐시) 관련 로직
    @Scheduled(cron = "0 0,30 * * * *")
    public void refreshAllCurrenciesCache() {
        List<String> currencies = List.of("JPY", "THB", "PHP", "VND", "USD", "EUR");
        for (String code : currencies) {
            refreshPopularItemsCache(code);
        }
    }
    // 2. 매시 정각/30분마다 자동 갱신
    public void refreshPopularItemsCache(String currencyCode) {
        log.info("{} 인기 아이템 캐시 갱신 시작", currencyCode);
        try {
            List<String> topKeywords = rankingService.getTopKeywords(6);
            var exchangeDetail = exchangeRateService.getExchangeDetail(currencyCode);
            if (exchangeDetail == null) return;
            double rate = exchangeDetail.getCurrentRate();

            List<UnifiedItemDto> items = topKeywords.stream().map(keyword -> {
                try {
                    Thread.sleep(500);

                    if ("JPY".equals(currencyCode)) {
                        var res = rakutenService.searchItems(keyword);
                        if (res == null || res.getItems().isEmpty()) return null;
                        var item = res.getItems().get(0).getItem();
                        return UnifiedItemDto.builder()
                                .id(item.getItemUrl()).name(item.getItemName()).price(item.getItemPrice())
                                .imageUrl(item.getMediumImageUrls().get(0).getImageUrl())
                                .source("RAKUTEN").exchangeRate(rate).build();
                    } else {
                        // SerpApi 결과 리스트 중 첫 번째 아이템을 추천템으로 선정
                        var resList = serpShopService.getRegionalProducts(keyword, currencyCode);
                        if (resList == null || resList.isEmpty()) return null;

                        var item = resList.get(0); // 첫 번째 상품
                        return UnifiedItemDto.builder()
                                .id(item.getLink()).name(item.getTitle()).price((int)item.getPrice())
                                .imageUrl(item.getThumbnail())
                                .source("SERP").exchangeRate(rate).build();
                    }
                } catch (Exception e) { return null; }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (!items.isEmpty()) {
                saveToCache(CACHE_KEY_PREFIX + currencyCode, items, 35);
            }
        } catch (Exception e) {
            log.error("{} 캐시 갱신 중 에러: ", currencyCode, e);
        }
    }

    // --- Redis 캐시 헬퍼 메서드 ---
    private List<UnifiedItemDto> getCachedItems(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, new TypeReference<List<UnifiedItemDto>>() {});
        } catch (Exception e) {
            log.error("Redis 읽기 에러: ", e);
            return null;
        }
    }

    private void saveToCache(String key, List<UnifiedItemDto> items, int minutes) {
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(key, json, minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis 저장 에러: ", e);
        }
    }
}