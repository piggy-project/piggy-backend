package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.NaverShopDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverShopService {

    private static final String INTERNAL_API_URL = "https://search.shopping.naver.com/ns/v1/branch/products";

    public NaverShopDto searchItems(String query) {
        try {
            // 1. 타임아웃 방어 설정 (Connect 2s, Read 3s)
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(3000);
            RestTemplate restTemplate = new RestTemplate(factory);

            // 2. 브라우저 위장 필수 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Referer", "https://search.shopping.naver.com/");
            headers.set("Accept", "application/json, text/plain, */*");

            // 3. Payload 파라미터 구성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("pagingIndex", 1);
            requestBody.put("pagingSize", 10);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 4. POST 요청 및 DTO 매핑
            ResponseEntity<NaverShopDto> response = restTemplate.exchange(
                    INTERNAL_API_URL,
                    HttpMethod.POST,
                    entity,
                    NaverShopDto.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("네이버 쇼핑 크롤링 처리 중 오류 - query: {}, error: {}", query, e.getMessage());
            return null;
        }
    }
}