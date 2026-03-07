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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerpShopService {

    @Value("${SERP_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // 리턴 타입을 List<SerpItemDto>로 변경
    public List<SerpItemDto> getRegionalProducts(String productName, String currencyCode) {
        // 1. 국가별 타겟 설정 (베트남 추가)
        String gl;
        String googleDomain;

        if ("THB".equals(currencyCode)) {
            gl = "th"; googleDomain = "google.co.th";
        } else if ("VND".equals(currencyCode)) {
            gl = "vn"; googleDomain = "google.com.vn";
        } else if ("CNY".equals(currencyCode)) {
            gl = "cn"; googleDomain = "google.com"; // 중국 본토는 google.com 기반 gl=cn 사용
        } else {
            gl = "ph"; googleDomain = "google.com.ph";
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_shopping")
                .queryParam("q", productName)
                .queryParam("google_domain", googleDomain)
                .queryParam("gl", gl)
                .queryParam("api_key", apiKey)
                .toUriString();

        List<SerpItemDto> results = new ArrayList<>();

        try {
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null || !root.has("shopping_results")) {
                log.warn("[SERP] 검색 결과가 없습니다.");
                return results;
            }

            JsonNode shoppingResults = root.path("shopping_results");
            for (JsonNode item : shoppingResults) {
                if (results.size() >= 10) break;

                String source = item.path("source").asText().toLowerCase();

                boolean isSouthEastAsia = List.of("THB", "PHP", "VND").contains(currencyCode);
                boolean isAllowedSource = !isSouthEastAsia || (source.contains("shopee") || source.contains("lazada"));

                if (isAllowedSource) {
                    String itemLink = item.path("link").asText();
                    if (itemLink.isEmpty() || itemLink.equals("#")) {
                        itemLink = item.path("product_link").asText("#");
                    }

                    results.add(SerpItemDto.builder()
                            .source(item.path("source").asText())
                            .title(item.path("title").asText())
                            .price(item.path("extracted_price").asDouble())
                            .link(itemLink)
                            .thumbnail(item.path("thumbnail").asText())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("[SERP] 오류 발생: ", e);
        }
        return results;
    }
}