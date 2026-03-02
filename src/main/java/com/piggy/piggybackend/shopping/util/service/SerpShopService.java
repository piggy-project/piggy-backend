package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerpShopService {

    @Value("${SERP_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public SerpItemDto getLowestRegionalPrice(String productName, String currencyCode) {
        log.info("[SERP] 가격 조회 시작 - 검색어: {}, 통화: {}", productName, currencyCode);
        // 1. 국가별 타겟 설정
        String gl = currencyCode.equals("THB") ? "th" : "ph";
        String googleDomain = currencyCode.equals("THB") ? "google.co.th" : "google.com.ph";

        String url = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_shopping")
                .queryParam("q", productName)
                .queryParam("google_domain", googleDomain)
                .queryParam("gl", gl)
                .queryParam("api_key", apiKey)
                .toUriString();
        log.debug("[SERP] 요청 URL: {}", url.replace(apiKey, "SECRET_KEY"));
        try {
            // RestTemplate 결과를 String으로 받아 ObjectMapper로 파싱하거나,
            // 바로 JsonNode로 받는 방식 모두 가능합니다.
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null || !root.has("shopping_results")) {
                log.warn("[SERP] API 응답이 비어있거나 검색 결과가 없습니다.");
                return null;
            }

            JsonNode shoppingResults = root.path("shopping_results");
            log.info("[SERP] 총 {}개의 쇼핑 결과를 수신했습니다.", shoppingResults.size());

            for (JsonNode item : shoppingResults) {
                String source = item.path("source").asText().toLowerCase();

                // 쇼피나 라자다 결과만 필터링
                if (source.contains("shopee") || source.contains("lazada")) {
                    log.info("[SERP] 최저가 발견 - 출처: {}, 가격: {}", source, item.path("extracted_price").asDouble());

                    return SerpItemDto.builder()
                            .source(item.path("source").asText())
                            .title(item.path("title").asText())
                            .price(item.path("extracted_price").asDouble())
                            .link(item.path("link").asText())
                            .thumbnail(item.path("thumbnail").asText())
                            .build();
                }
            }
            log.warn("[SERP] 검색 결과는 있으나 Shopee 또는 Lazada 상품이 없습니다.");
        } catch (Exception e) {
            log.error("동남아 지역 가격 조회 중 오류 발생 (통화: {}): ", currencyCode, e);
        }
        return null;
    }
}