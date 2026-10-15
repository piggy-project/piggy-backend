package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.NaverShopDto;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// 네이버 쇼핑 검색 API가 2026-07-31 종료되어 SerpAPI(Google Shopping 한국) 결과로 대체.
// 프론트 호환을 위해 기존 NaverShopDto 형태로 매핑해서 반환
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverShopService {

    private final SerpShopService serpShopService;

    public NaverShopDto searchItems(String query) {
        try {
            List<SerpItemDto> items = serpShopService.getKoreanProducts(query);

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
}
