package com.piggy.piggybackend.shopping.util.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerpItemDto {
    private String source;     // 판매처 (Shopee, Lazada 등)
    private String title;      // 상품명
    private double price;      // 현지 통화 가격 (숫자)
    private String link;       // 상품 상세 페이지 링크
    private String thumbnail;  // 상품 이미지 URL
}