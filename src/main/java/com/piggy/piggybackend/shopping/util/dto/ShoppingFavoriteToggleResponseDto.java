package com.piggy.piggybackend.shopping.util.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ShoppingFavoriteToggleResponseDto {
    private boolean favorite;
    private ShoppingFavoriteResponseDto item;
}
