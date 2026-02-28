package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.RakutenItemDto;
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
public class RakutenService {

    @Value("${rakuten.api.application-id}")
    private String applicationId;

    @Value("${rakuten.api.url}")
    private String apiUrl;

    @Value("${rakuten.api.access-key}")
    private String accessKey;

    public RakutenItemDto searchItems(String keyword) {
        RestTemplate restTemplate = new RestTemplate();

        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("applicationId", applicationId.trim())
                .queryParam("accessKey", accessKey.trim())
                .queryParam("keyword", keyword)
                .queryParam("format", "json")
                .queryParam("hits", 10)
                .queryParam("sort", "standard")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        log.info("라쿠텐 요청 URI: {}", uri);

        return restTemplate.getForObject(uri, RakutenItemDto.class);
    }
}
