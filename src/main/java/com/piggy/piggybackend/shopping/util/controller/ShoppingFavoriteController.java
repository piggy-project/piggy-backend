package com.piggy.piggybackend.shopping.util.controller;

import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteRequestDto;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteResponseDto;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteToggleResponseDto;
import com.piggy.piggybackend.shopping.util.service.ShoppingFavoriteService;
import com.piggy.piggybackend.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shopping/favorites")
public class ShoppingFavoriteController {

    private final ShoppingFavoriteService shoppingFavoriteService;

    @GetMapping
    public ResponseEntity<List<ShoppingFavoriteResponseDto>> getFavorites(@LoginUser User user) {
        return ResponseEntity.ok(shoppingFavoriteService.getFavorites(user));
    }

    @PostMapping
    public ResponseEntity<ShoppingFavoriteResponseDto> addFavorite(
            @LoginUser User user,
            @Valid @RequestBody ShoppingFavoriteRequestDto request) {
        return ResponseEntity.ok(shoppingFavoriteService.addFavorite(user, request));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ShoppingFavoriteToggleResponseDto> toggleFavorite(
            @LoginUser User user,
            @Valid @RequestBody ShoppingFavoriteRequestDto request) {
        return ResponseEntity.ok(shoppingFavoriteService.toggleFavorite(user, request));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @LoginUser User user,
            @RequestParam String source,
            @RequestParam String externalProductId) {
        return ResponseEntity.ok(Map.of(
                "favorite", shoppingFavoriteService.isFavorite(user, source, externalProductId)
        ));
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> deleteFavorite(
            @PathVariable Long favoriteId,
            @LoginUser User user) {
        shoppingFavoriteService.deleteFavorite(favoriteId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFavoriteByProduct(
            @LoginUser User user,
            @RequestParam String source,
            @RequestParam String externalProductId) {
        shoppingFavoriteService.deleteFavoriteByProduct(user, source, externalProductId);
        return ResponseEntity.noContent().build();
    }
}
