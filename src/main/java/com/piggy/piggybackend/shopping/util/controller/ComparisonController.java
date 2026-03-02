package com.piggy.piggybackend.shopping.util.controller;

import com.piggy.piggybackend.shopping.util.dto.ComparisonResponse;
import com.piggy.piggybackend.shopping.util.dto.UnifiedItemDto;
import com.piggy.piggybackend.shopping.util.service.ComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shopping")
@RequiredArgsConstructor
public class ComparisonController {
    private final ComparisonService comparisonService;

    // 상품 검색 및 가격 목록 출력
    @GetMapping("/compare")
    public ResponseEntity<ComparisonResponse> compare(
            @RequestParam String query,
            @RequestParam String currencyCode
    ) {
        return ResponseEntity.ok(comparisonService.comparePrices(query, currencyCode));
    }

    // Redis 기반 인기 상품 추천
    @GetMapping("/popular")
    public ResponseEntity<List<UnifiedItemDto>> getPopular() {
        return ResponseEntity.ok(comparisonService.getPopularItems());
    }
}