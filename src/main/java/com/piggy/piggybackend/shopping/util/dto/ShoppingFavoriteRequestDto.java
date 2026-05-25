package com.piggy.piggybackend.shopping.util.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShoppingFavoriteRequestDto {

    @NotBlank
    @Size(max = 500)
    private String externalProductId;

    @NotBlank
    @Size(max = 50)
    private String source;

    @NotBlank
    @Size(max = 500)
    private String name;

    @NotNull
    @PositiveOrZero
    private Integer price;

    @Size(max = 1000)
    private String imageUrl;

    @Size(max = 1000)
    private String productUrl;

    @Size(max = 100)
    private String shopName;

    @Size(max = 10)
    private String currencyCode;
    private Double exchangeRate;
}
