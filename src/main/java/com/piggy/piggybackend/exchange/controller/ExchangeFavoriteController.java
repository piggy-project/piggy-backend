package com.piggy.piggybackend.exchange.controller;

import com.piggy.piggybackend.exchange.dto.ExchangeFavoriteResponseDto;
import com.piggy.piggybackend.exchange.service.ExchangeFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange/favorites")
@RequiredArgsConstructor
public class ExchangeFavoriteController {

    private final ExchangeFavoriteService favoriteService;

    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** 즐겨찾기 목록 + 현재 환율 조회 */
    @GetMapping
    public ResponseEntity<List<ExchangeFavoriteResponseDto>> getFavorites() {
        List<ExchangeFavoriteResponseDto> result = favoriteService.getFavorites(getEmail());
        return ResponseEntity.ok(result);
    }

    /** 즐겨찾기 추가/제거 토글 */
    @PostMapping("/{currencyCode}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String currencyCode) {
        boolean added = favoriteService.toggleFavorite(getEmail(), currencyCode);
        return ResponseEntity.ok(Map.of("currencyCode", currencyCode, "favorited", added));
    }

    /** 특정 통화 즐겨찾기 여부 확인 */
    @GetMapping("/{currencyCode}")
    public ResponseEntity<Map<String, Object>> isFavorite(@PathVariable String currencyCode) {
        boolean favorited = favoriteService.isFavorite(getEmail(), currencyCode);
        return ResponseEntity.ok(Map.of("currencyCode", currencyCode, "favorited", favorited));
    }
}