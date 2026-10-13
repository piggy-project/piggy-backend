package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.ElevenShopDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElevenShopService {

    @Value("${elevenst.api.key}")
    private String apiKey;

    @Value("${elevenst.api.url:http://openapi.11st.co.kr/openapi/OpenApiService.tmall}")
    private String apiUrl;

    public ElevenShopDto searchItems(String keyword) {
        RestTemplate restTemplate = new RestTemplate();

        // 11번가 상품검색 API URL 구성
        // http://openapi.11st.co.kr/openapi/OpenApiService.tmall?key=[key]&apiCode=ProductSearch&keyword=[keyword]
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("apiCode", "ProductSearch")
                .queryParam("keyword", keyword)
                .queryParam("pageSize", 10) // 필요한 상품 개수 지정
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        try {
            log.info("11번가 API 요청: keyword={}", keyword);
            return restTemplate.getForObject(uri, ElevenShopDto.class);
        } catch (Exception e) {
            log.error("11번가 API 호출 에러 (keyword={}): ", keyword, e);
            return null;
        }
    }
}