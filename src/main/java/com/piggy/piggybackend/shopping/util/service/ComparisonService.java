package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import com.piggy.piggybackend.global.redis.service.SearchRankingService;
import com.piggy.piggybackend.shopping.util.dto.ComparisonResponse;
import com.piggy.piggybackend.shopping.util.dto.RakutenItemDto;
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

    private static final String CACHE_KEY = "popular_items_cache";

    public ComparisonResponse comparePrices(String query) {
        try {
            rankingService.incrementKeywordCount(query);

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

    // 인기 키워드 기반 추천 아이템 가져오기
    public List<UnifiedItemDto> getPopularItems() {
        List<UnifiedItemDto> cached = getCachedItems(CACHE_KEY);
        if (cached == null || cached.isEmpty()) {
            refreshPopularItemsCache(); // 캐시 없으면 즉시 생성
            return getCachedItems(CACHE_KEY);
        }
        return cached;
    }
    // 2. 매시 정각/30분마다 자동 갱신
    @Scheduled(cron = "0 0,30 * * * *")
    public void refreshPopularItemsCache() {
        List<String> topKeywords = rankingService.getTopKeywords(3);
        double jpyRate = exchangeRateService.getExchangeDetail("JPY").getCurrentRate();

        List<UnifiedItemDto> items = topKeywords.stream().map(keyword -> {
            try {
                Thread.sleep(1000); // 1초 간격으로 라쿠텐 API 호출 (Rate Limit 방어)
                var result = rakutenService.searchItems(keyword);
                if (result == null || result.getItems().isEmpty()) return null;

                var item = result.getItems().get(0).getItem();
                return UnifiedItemDto.builder()
                        .id(item.getItemUrl())
                        .name(item.getItemName())
                        .price(item.getItemPrice())
                        .imageUrl(item.getMediumImageUrls().get(0).getImageUrl())
                        .source("RAKUTEN")
                        .exchangeRate(jpyRate)
                        .build();
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (!items.isEmpty()) saveToCache(CACHE_KEY, items, 35);
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