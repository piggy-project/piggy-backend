package com.piggy.piggybackend.exchange.controller;


import com.piggy.piggybackend.exchange.dto.TravelRecommendationResponseDto;
import com.piggy.piggybackend.exchange.service.TravelRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class TravelRecommendationController {

    private final TravelRecommendationService travelRecommendationService;

    @GetMapping("/travel-recommendations")
    public ResponseEntity<List<TravelRecommendationResponseDto>> getRecommendations() {
        return ResponseEntity.ok(travelRecommendationService.getRecommendations());
    }
    // 레디스 초기화하는 경로
    @GetMapping("/travel-recommendations/clear")
    public ResponseEntity<String> clearCache() {
        travelRecommendationService.clearCache();
        return ResponseEntity.ok("Cache Cleared");
    }

    // 여행지 사진 (위키미디어가 앱의 직접 요청을 막아서 서버가 대신 받아 준다)
    @GetMapping("/travel-image/{slug}")
    public ResponseEntity<byte[]> getTravelImage(@PathVariable String slug) {
        TravelRecommendationService.TravelImage image = travelRecommendationService.loadImage(slug);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(image.bytes());
    }
}