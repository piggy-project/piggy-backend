package com.piggy.piggybackend.shopping.util.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopDto {
    private List<ProductData> data;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductData {
        private String id;
        private String productName;
        private int salePrice;
        private String mallName;
        private ProductUrl productUrl;
        private List<ImageInfo> images;

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProductUrl {
            private String pcUrl;
            private String mobileUrl;
        }

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImageInfo {
            private String imageUrl;
        }
    }
}