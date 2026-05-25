package com.piggy.piggybackend.shopping.util.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ShoppingFavoriteResponseDto {
    private Long id;
    private String externalProductId;
    private String source;
    private String name;
    private int price;
    private String imageUrl;
    private String productUrl;
    private String shopName;
    private String currencyCode;
    private Double exchangeRate;
    private LocalDateTime createdAt;
}
