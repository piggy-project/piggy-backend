package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.piggy.piggybackend.global.redis.service.ApiQuotaService;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Service
public class SerpShopService {

    @Value("${SERP_API_KEY}")
    private String apiKey;

    @Value("${serp.daily-limit:200}")
    private int dailyLimit;

    private final ApiQuotaService apiQuotaService;
    private final RestTemplate restTemplate;

    public SerpShopService(ApiQuotaService apiQuotaService) {
        this.apiQuotaService = apiQuotaService;

        // SerpAPI는 응답이 수 초 걸릴 수 있어 Read 타임아웃을 넉넉히 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    // 국내(한국) 쇼핑 검색 - 네이버 쇼핑 API 대체
    public List<SerpItemDto> getKoreanProducts(String productName) {
        return search(productName, "kr", "google.co.kr", "ko", item -> true);
    }

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

        // 동남아는 Shopee, Lazada 결과만 사용
        boolean isSouthEastAsia = List.of("THB", "PHP", "VND").contains(currencyCode);
        Predicate<JsonNode> sourceFilter = item -> {
            String source = item.path("source").asText().toLowerCase();
            return !isSouthEastAsia || source.contains("shopee") || source.contains("lazada");
        };

        return search(productName, gl, googleDomain, null, sourceFilter);
    }

    private List<SerpItemDto> search(String productName, String gl, String googleDomain, String hl,
                                     Predicate<JsonNode> sourceFilter) {
        List<SerpItemDto> results = new ArrayList<>();

        // 크레딧 소진 방지: 일일 호출 한도 초과 시 호출하지 않음
        if (!apiQuotaService.tryAcquireDaily("SERP", dailyLimit)) {
            return results;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_shopping")
                .queryParam("q", productName)
                .queryParam("google_domain", googleDomain)
                .queryParam("gl", gl)
                .queryParam("api_key", apiKey);
        if (hl != null) {
            builder.queryParam("hl", hl);
        }
        String url = builder.toUriString();

        try {
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null || !root.has("shopping_results")) {
                log.warn("[SERP] 검색 결과가 없습니다. (gl: {})", gl);
                return results;
            }

            JsonNode shoppingResults = root.path("shopping_results");
            for (JsonNode item : shoppingResults) {
                if (results.size() >= 10) break;

                if (sourceFilter.test(item)) {
                    String itemLink = item.path("link").asText();
                    if (itemLink.isEmpty() || itemLink.equals("#")) {
                        itemLink = item.path("product_link").asText("#");
                    }

                    results.add(SerpItemDto.builder()
                            .productId(item.path("product_id").asText(itemLink))
                            .source(item.path("source").asText())
                            .title(item.path("title").asText())
                            .price(item.path("extracted_price").asDouble())
                            .link(itemLink)
                            .thumbnail(item.path("thumbnail").asText())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("[SERP] 오류 발생 (gl: {}): ", gl, e);
        }
        return results;
    }
}
