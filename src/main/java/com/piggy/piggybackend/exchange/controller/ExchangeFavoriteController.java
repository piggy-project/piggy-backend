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

    private boolean isAnonymous(String email) {
        return email == null || email.equals("anonymousUser");
    }

    /** 즐겨찾기 목록 + 현재 환율 조회 */
    @GetMapping
    public ResponseEntity<?> getFavorites() {
        String email = getEmail();
        if (isAnonymous(email)) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<ExchangeFavoriteResponseDto> result = favoriteService.getFavorites(email);
        return ResponseEntity.ok(result);
    }

    /** 즐겨찾기 추가/제거 토글 */
    @PostMapping("/{currencyCode}")
    public ResponseEntity<?> toggleFavorite(@PathVariable String currencyCode) {
        String email = getEmail();
        if (isAnonymous(email)) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean added = favoriteService.toggleFavorite(email, currencyCode);
        return ResponseEntity.ok(Map.of("currencyCode", currencyCode, "favorited", added));
    }

    /** 특정 통화 즐겨찾기 여부 확인 */
    @GetMapping("/{currencyCode}")
    public ResponseEntity<?> isFavorite(@PathVariable String currencyCode) {
        String email = getEmail();
        if (isAnonymous(email)) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean favorited = favoriteService.isFavorite(email, currencyCode);
        return ResponseEntity.ok(Map.of("currencyCode", currencyCode, "favorited", favorited));
    }
}