package com.piggy.piggybackend.shopping.util.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 국내 쇼핑 검색 결과 (네이버 쇼핑 API 종료 후 SerpAPI 한국 검색 결과를 같은 형태로 매핑)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopDto {
    private List<ProductData> data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductData {
        private String id;
        private String productName;
        private int salePrice;
        private String mallName;
        private ProductUrl productUrl;
        private List<ImageInfo> images;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProductUrl {
            private String pcUrl;
            private String mobileUrl;
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImageInfo {
            private String imageUrl;
        }
    }
}
