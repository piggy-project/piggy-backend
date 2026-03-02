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

    private static final String CACHE_KEY = "popular_items_cache";

    public ComparisonResponse comparePrices(String query, String currencyCode) {
        try {
            // 기존 검색어 랭킹 로직 유지
            rankingService.incrementKeywordCount(query);

            // 1. 네이버 API 호출 (한국 가격은 항상 공통)
            var naverResult = naverService.searchItems(query);

            // 2. 외화 결과 분기 처리 (통화 코드에 따라 분기)
            RakutenItemDto rakutenResult = null;
            SerpItemDto serpResult = null;

            if ("JPY".equals(currencyCode)) {
                // 일본일 경우 기존처럼 라쿠텐 호출
                rakutenResult = rakutenService.searchItems(query);
            } else if ("THB".equals(currencyCode) || "PHP".equals(currencyCode)) {
                // 동남아일 경우 SerpShopService 호출 (SerpItemDto 반환)
                serpResult = serpShopService.getLowestRegionalPrice(query, currencyCode);
            }

            // 3. 환율 데이터 확인 (입력받은 currencyCode에 맞춰 동적 조회)
            var exchangeDetail = exchangeRateService.getExchangeDetail(currencyCode);
            if (exchangeDetail == null) {
                throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
            }
            double exchangeRate = exchangeDetail.getCurrentRate();

            // 4. 빌더 패턴에 맞춰 결과 조립
            return ComparisonResponse.builder()
                    .naverResults(naverResult)
                    .rakutenResults(rakutenResult)
                    .serpResults(serpResult)  // 추가된 SerpItemDto 결과
                    .exchangeRate(exchangeRate)
                    .currencyCode(currencyCode)
                    .build();

        } catch (CustomException e) {
            // 이미 정의된 예외는 그대로 전달 (기존 유지)
            throw e;
        } catch (Exception e) {
            // 예상치 못한 에러 로깅 및 공통 에러 반환 (기존 유지)
            log.error("검색 중 예상치 못한 에러 (통화: {}): ", currencyCode, e);
            throw new CustomException(ErrorCode.SHOPPING_API_ERROR);
        }
    }

    // 인기 아이템 리스트 (캐시 읽기)
    public List<UnifiedItemDto> getPopularItems() {
        try {
            List<UnifiedItemDto> cached = getCachedItems(CACHE_KEY);
            if (cached == null || cached.isEmpty()) {
                refreshPopularItemsCache();
                cached = getCachedItems(CACHE_KEY);
            }

            if (cached == null || cached.isEmpty()) {
                throw new CustomException(ErrorCode.EMPTY_SEARCH_RESULT);
            }
            return cached;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    // 2. 매시 정각/30분마다 자동 갱신
    @Scheduled(cron = "0 0,30 * * * *")
    public void refreshPopularItemsCache() {
        log.info("인기 아이템 캐시 갱신 시작");
        try {
            List<String> topKeywords = rankingService.getTopKeywords(6);

            // 환율 정보가 없으면 작업 중단
            var jpyDetail = exchangeRateService.getExchangeDetail("JPY");
            if (jpyDetail == null) {
                log.error("캐시 갱신 실패: 환율 데이터를 찾을 수 없습니다.");
                return;
            }
            double jpyRate = jpyDetail.getCurrentRate();

            List<UnifiedItemDto> items = topKeywords.stream().map(keyword -> {
                try {
                    Thread.sleep(1000);
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
                    log.warn("키워드 [{}] 검색 실패: {}", keyword, e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (!items.isEmpty()) {
                saveToCache(CACHE_KEY, items, 35);
                log.info("캐시 갱신 완료 (총 {}개 아이템)", items.size());
            }
        } catch (Exception e) {
            log.error("스케줄러 실행 중 심각한 에러: ", e);
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