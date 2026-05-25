package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteRequestDto;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteResponseDto;
import com.piggy.piggybackend.shopping.util.dto.ShoppingFavoriteToggleResponseDto;
import com.piggy.piggybackend.shopping.util.entity.ShoppingFavorite;
import com.piggy.piggybackend.shopping.util.repository.ShoppingFavoriteRepository;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingFavoriteService {

    private final ShoppingFavoriteRepository shoppingFavoriteRepository;

    @Transactional(readOnly = true)
    public List<ShoppingFavoriteResponseDto> getFavorites(User user) {
        validateLogin(user);

        return shoppingFavoriteRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShoppingFavoriteResponseDto addFavorite(User user, ShoppingFavoriteRequestDto request) {
        validateLogin(user);

        return shoppingFavoriteRepository.findByUserAndSourceAndExternalProductId(
                        user, request.getSource(), request.getExternalProductId())
                .map(this::toResponse)
                .orElseGet(() -> toResponse(shoppingFavoriteRepository.save(toEntity(user, request))));
    }

    @Transactional
    public ShoppingFavoriteToggleResponseDto toggleFavorite(User user, ShoppingFavoriteRequestDto request) {
        validateLogin(user);

        return shoppingFavoriteRepository.findByUserAndSourceAndExternalProductId(
                        user, request.getSource(), request.getExternalProductId())
                .map(favorite -> {
                    shoppingFavoriteRepository.delete(favorite);
                    return ShoppingFavoriteToggleResponseDto.builder()
                            .favorite(false)
                            .item(null)
                            .build();
                })
                .orElseGet(() -> {
                    ShoppingFavoriteResponseDto saved = toResponse(
                            shoppingFavoriteRepository.save(toEntity(user, request)));
                    return ShoppingFavoriteToggleResponseDto.builder()
                            .favorite(true)
                            .item(saved)
                            .build();
                });
    }

    @Transactional
    public void deleteFavorite(Long favoriteId, User user) {
        validateLogin(user);

        ShoppingFavorite favorite = shoppingFavoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        shoppingFavoriteRepository.delete(favorite);
    }

    @Transactional
    public void deleteFavoriteByProduct(User user, String source, String externalProductId) {
        validateLogin(user);

        ShoppingFavorite favorite = shoppingFavoriteRepository.findByUserAndSourceAndExternalProductId(
                        user, source, externalProductId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        shoppingFavoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(User user, String source, String externalProductId) {
        validateLogin(user);
        return shoppingFavoriteRepository.existsByUserAndSourceAndExternalProductId(user, source, externalProductId);
    }

    private void validateLogin(User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }
    }

    private ShoppingFavorite toEntity(User user, ShoppingFavoriteRequestDto request) {
        return ShoppingFavorite.builder()
                .user(user)
                .externalProductId(request.getExternalProductId())
                .source(request.getSource())
                .name(request.getName())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .productUrl(request.getProductUrl())
                .shopName(request.getShopName())
                .currencyCode(request.getCurrencyCode())
                .exchangeRate(request.getExchangeRate())
                .build();
    }

    private ShoppingFavoriteResponseDto toResponse(ShoppingFavorite favorite) {
        return ShoppingFavoriteResponseDto.builder()
                .id(favorite.getId())
                .externalProductId(favorite.getExternalProductId())
                .source(favorite.getSource())
                .name(favorite.getName())
                .price(favorite.getPrice())
                .imageUrl(favorite.getImageUrl())
                .productUrl(favorite.getProductUrl())
                .shopName(favorite.getShopName())
                .currencyCode(favorite.getCurrencyCode())
                .exchangeRate(favorite.getExchangeRate())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
