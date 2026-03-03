package com.piggy.piggybackend.shopping.util.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnifiedItemDto {
    private String id;
    private String name;
    private int price;
    private String imageUrl;
    private String shopName;
    private String source;     // "NAVER" 또는 "RAKUTEN"
    private double exchangeRate;
}
