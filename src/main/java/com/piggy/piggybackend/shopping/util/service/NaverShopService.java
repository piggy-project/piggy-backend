package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.NaverShopDto;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// 네이버 쇼핑 검색 API가 2026-07-31 종료되어 SerpAPI(Google Shopping 한국) 결과로 대체.
// 프론트 호환을 위해 기존 NaverShopDto 형태로 매핑해서 반환
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverShopService {

    private final SerpShopService serpShopService;

    // 가격 신뢰도 필터 기준
    private static final int MIN_VALID_PRICE = 100;          // 이 값 미만은 파싱 오류/비정상 가격으로 간주
    private static final double OUTLIER_RATIO = 0.35;        // 중앙값의 35% 미만은 이상값으로 제거
    private static final int MIN_ITEMS_FOR_MEDIAN = 3;       // 중앙값 필터를 적용할 최소 결과 수

    public NaverShopDto searchItems(String query) {
        try {
            List<SerpItemDto> items = filterReliableItems(query, serpShopService.getKoreanProducts(query));

            List<NaverShopDto.ProductData> data = items.stream()
                    .map(item -> NaverShopDto.ProductData.builder()
                            .id(item.getProductId())
                            .productName(item.getTitle())
                            .salePrice((int) item.getPrice())
                            .mallName(item.getSource())
                            .productUrl(NaverShopDto.ProductData.ProductUrl.builder()
                                    .pcUrl(item.getLink())
                                    .mobileUrl(item.getLink())
                                    .build())
                            .images(List.of(NaverShopDto.ProductData.ImageInfo.builder()
                                    .imageUrl(item.getThumbnail())
                                    .build()))
                            .build())
                    .toList();

            return NaverShopDto.builder().data(data).build();

        } catch (Exception e) {
            log.error("국내 쇼핑 검색 처리 중 오류 - query: {}, error: {}", query, e.getMessage());
            return null;
        }
    }

    // 1) 비정상 가격 제거 → 2) 제목 관련도 필터 → 3) 중앙값 기반 이상값 제거
    private List<SerpItemDto> filterReliableItems(String query, List<SerpItemDto> items) {
        if (items == null || items.isEmpty()) return List.of();

        // 가격 하한 가드 (0원, 9원 등)
        List<SerpItemDto> valid = items.stream()
                .filter(item -> item.getPrice() >= MIN_VALID_PRICE)
                .toList();

        // 제목에 검색어 단어가 하나도 없는 결과 제외. 전부 걸러지면(예: 영어 검색어 vs 한글 제목) 이 필터는 건너뜀
        List<String> tokens = Arrays.stream(query.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .toList();
        List<SerpItemDto> relevant = valid.stream()
                .filter(item -> {
                    String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase(Locale.ROOT);
                    return tokens.stream().anyMatch(title::contains);
                })
                .toList();
        if (!relevant.isEmpty()) {
            valid = relevant;
        }

        // 중앙값보다 지나치게 싼 결과(스티커, 장난감, 단위가격 등) 제거
        if (valid.size() >= MIN_ITEMS_FOR_MEDIAN) {
            double[] sorted = valid.stream().mapToDouble(SerpItemDto::getPrice).sorted().toArray();
            int mid = sorted.length / 2;
            double median = sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
            double threshold = median * OUTLIER_RATIO;
            valid = valid.stream()
                    .filter(item -> item.getPrice() >= threshold)
                    .toList();
        }

        log.info("국내 검색 결과 필터링 - query: {}, 원본 {}건 → {}건", query, items.size(), valid.size());
        return valid;
    }
}
