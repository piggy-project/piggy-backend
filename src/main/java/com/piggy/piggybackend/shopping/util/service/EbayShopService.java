package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbayShopService {

    @Value("${ebay.client-id}")
    private String clientId;

    @Value("${ebay.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final StringRedisTemplate redisTemplate; // Redis 주입

    private static final String EBAY_TOKEN_KEY = "ebay_access_token"; // Redis 키

    // 1. OAuth 2.0 토큰 발급 및 Redis 캐싱 로직
    private String getAccessToken() {
        // Redis에서 먼저 토큰을 꺼내봅니다.
        String cachedToken = redisTemplate.opsForValue().get(EBAY_TOKEN_KEY);
        if (cachedToken != null) {
            log.info("[eBay] Redis에서 캐싱된 토큰을 사용합니다.");
            return cachedToken;
        }

        // Redis에 없으면 새로 발급받습니다. (동기화 블록으로 중복 요청 방지)
        synchronized (this) {
            // 더블 체크 (앞선 요청이 토큰을 이미 채웠을 수 있음)
            String doubleCheckToken = redisTemplate.opsForValue().get(EBAY_TOKEN_KEY);
            if (doubleCheckToken != null) return doubleCheckToken;

            log.info("[eBay] 새로운 Access Token 발급 시도");
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "https://api.ebay.com/oauth/api_scope");

            try {
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                        "https://api.ebay.com/identity/v1/oauth2/token", request, JsonNode.class);

                JsonNode responseBody = response.getBody();
                String token = responseBody.path("access_token").asText();

                // 발급받은 토큰을 Redis에 저장 (유효 시간 설정)
                int expiresIn = responseBody.path("expires_in").asInt(3600);
                // 안전하게 만료 5분 전(300초)까지만 유효하도록 설정
                redisTemplate.opsForValue().set(EBAY_TOKEN_KEY, token, expiresIn - 300, TimeUnit.SECONDS);

                return token;
            } catch (Exception e) {
                log.error("[eBay] 토큰 발급 실패: {}", e.getMessage());
                throw new RuntimeException("eBay Authentication Failed");
            }
        }
    }

    // 2. 상품 검색 (이 부분은 기존과 거의 동일하지만 getAccessToken()을 호출하는 구조는 유지)
    public List<SerpItemDto> getEbayProducts(String query, String currencyCode) {
        log.info("[eBay] 상품 검색 시작 - 검색어: {}, 통화: {}", query, currencyCode);

        try {
            String token = getAccessToken();
            String marketplaceId = "USD".equals(currencyCode) ? "EBAY_US" : "EBAY_DE";

            String url = UriComponentsBuilder.fromHttpUrl("https://api.ebay.com/buy/browse/v1/item_summary/search")
                    .queryParam("q", query)
                    .queryParam("limit", 10)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("X-EBAY-C-MARKETPLACE-ID", marketplaceId);
            headers.set("Content-Language", "USD".equals(currencyCode) ? "en-US" : "de-DE");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode summaries = response.getBody().path("itemSummaries");
            List<SerpItemDto> results = new ArrayList<>();

            if (summaries.isArray()) {
                for (JsonNode item : summaries) {
                    String priceStr = item.path("price").path("value").asText("0");
                    results.add(SerpItemDto.builder()
                            .source("eBay (" + marketplaceId + ")")
                            .title(item.path("title").asText("No Title"))
                            .price(Double.parseDouble(priceStr))
                            .link(item.path("itemWebUrl").asText("#"))
                            .thumbnail(item.path("image").path("imageUrl").asText(""))
                            .build());
                }
            }
            log.info("[eBay] {}개의 상품을 성공적으로 가져왔습니다.", results.size());
            return results;

        } catch (Exception e) {
            log.error("[eBay] 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }
}