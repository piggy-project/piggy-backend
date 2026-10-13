package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.redis.service.SearchRankingService;
import com.piggy.piggybackend.shopping.util.dto.*;
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

    private final NaverShopService naverShopService;
    private final RakutenService rakutenService;
    private final ExchangeRateService exchangeRateService;
    private final SearchRankingService rankingService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SerpShopService serpShopService;
    private final EbayShopService ebayShopService;

    private static final String CACHE_KEY_PREFIX = "popular_items_cache:";
    private static final String SEARCH_CACHE_PREFIX = "search_results_cache:";

    public ComparisonResponse comparePrices(String query, String currencyCode) {
        String searchCacheKey = SEARCH_CACHE_PREFIX + currencyCode + ":" + query;
        ComparisonResponse cachedResponse = getCachedResponse(searchCacheKey);

        if (cachedResponse != null) {
            log.info("API 호출 없이 결과 반환: {}", query);
            return cachedResponse;
        }

        try {
            rankingService.incrementKeywordCount(query, currencyCode);

            NaverShopDto naverResult = naverShopService.searchItems(query);

            RakutenItemDto rakutenResult = null;
            List<SerpItemDto> serpResults = null;

            if ("JPY".equals(currencyCode)) {
                rakutenResult = rakutenService.searchItems(query);
            }
            else if (List.of("USD", "EUR", "GBP", "AUD").contains(currencyCode)) {
                serpResults = ebayShopService.getEbayProducts(query, currencyCode);
            }
            else if (List.of("THB", "PHP", "VND", "CNY").contains(currencyCode)) {
                serpResults = serpShopService.getRegionalProducts(query, currencyCode);
            }

            var exchangeDetail = exchangeRateService.getExchangeDetail(currencyCode);
            if (exchangeDetail == null) {
                throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
            }
            double exchangeRate = exchangeDetail.getCurrentRate();

            ComparisonResponse response = ComparisonResponse.builder()
                    .naverResults(naverResult)
                    .rakutenResults(rakutenResult)
                    .serpResults(serpResults)
                    .exchangeRate(exchangeRate)
                    .currencyCode(currencyCode)
                    .build();

            saveResponseToCache(searchCacheKey, response, 60);

            return response;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("검색 중 예상치 못한 에러 (통화: {}): ", currencyCode, e);
            throw new CustomException(ErrorCode.SHOPPING_API_ERROR);
        }
    }

    // 인기 아이템 리스트 (캐시 읽기)
    public List<UnifiedItemDto> getPopularItems(String currencyCode) {
        String cacheKey = CACHE_KEY_PREFIX + currencyCode;
        try {
            List<UnifiedItemDto> cached = getCachedItems(cacheKey);
            if (cached == null || cached.isEmpty()) {
                return List.of();
            }
            return cached;
        } catch (Exception e) {
            log.error("추천 아이템 조회 에러: ", e);
            return List.of();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void refreshAllCurrenciesCache() {
        List<String> currencies = List.of("JPY", "CNY", "USD", "VND", "PHP", "THB", "EUR", "AUD", "GBP");
        for (String code : currencies) {
            refreshPopularItemsCache(code);
        }
    }

    public void refreshPopularItemsCache(String currencyCode) {
        log.info("{} 인기 아이템 캐시 갱신 시작", currencyCode);
        try {
            int limit = (List.of("THB", "PHP", "VND", "CNY").contains(currencyCode)) ? 2 : 6;
            List<String> topKeywords = rankingService.getTopKeywords(currencyCode, limit);

            if (topKeywords.isEmpty()) {
                topKeywords = rankingService.getTopKeywords("ALL", limit);
            }

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
                    }
                    else if ("USD".equals(currencyCode) || "EUR".equals(currencyCode)) {
                        var resList = ebayShopService.getEbayProducts(keyword, currencyCode);
                        if (resList == null || resList.isEmpty()) return null;

                        var item = resList.get(0);
                        return UnifiedItemDto.builder()
                                .id(item.getLink())
                                .name(item.getTitle())
                                .price((int) item.getPrice())
                                .imageUrl(item.getThumbnail())
                                .source("EBAY")
                                .exchangeRate(rate)
                                .build();
                    }
                    else {
                        var resList = serpShopService.getRegionalProducts(keyword, currencyCode);
                        if (resList == null || resList.isEmpty()) return null;

                        var item = resList.get(0);
                        return UnifiedItemDto.builder()
                                .id(item.getLink()).name(item.getTitle()).price((int)item.getPrice())
                                .imageUrl(item.getThumbnail())
                                .source("SERP").exchangeRate(rate).build();
                    }
                } catch (Exception e) { return null; }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (!items.isEmpty()) {
                saveToCache(CACHE_KEY_PREFIX + currencyCode, items, 1500);
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
    private ComparisonResponse getCachedResponse(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return (json != null) ? objectMapper.readValue(json, ComparisonResponse.class) : null;
        } catch (Exception e) {
            return null;
        }
    }
    private void saveResponseToCache(String key, ComparisonResponse response, int minutes) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("검색 결과 캐시 저장 에러: ", e);
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