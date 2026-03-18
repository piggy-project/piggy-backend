package com.piggy.piggybackend.exchange.controller;


import com.piggy.piggybackend.exchange.dto.TravelRecommendationResponseDto;
import com.piggy.piggybackend.exchange.service.TravelRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}