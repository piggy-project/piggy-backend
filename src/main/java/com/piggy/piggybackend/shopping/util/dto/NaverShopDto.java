package com.piggy.piggybackend.shopping.util.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NaverShopDto {
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    public static class Item {
        private String title;      // 상품명
        private String link;       // 상품 링크
        private String image;      // 상품 이미지 URL
        private int lprice;        // 최저가
        private String mallName;   // 판매처
        private String productId;  // 네이버 상품 ID
    }
}
