package com.piggy.piggybackend.shopping.util.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RakutenItemDto {
    @JsonProperty("Items")
    private List<ItemWrapper> items;

    @Getter
    @NoArgsConstructor
    public static class ItemWrapper {
        @JsonProperty("Item")
        private ItemDetail item;
    }

    @Getter
    @NoArgsConstructor
    public static class ItemDetail {
        private String itemName;      // 상품명
        private int itemPrice;        // 가격 (JPY)
        private String itemUrl;       // 상품 링크
        private String shopName;      // 판매처

        @JsonProperty("mediumImageUrls")
        private List<ImageUrl> mediumImageUrls;

        @Getter
        @NoArgsConstructor
        public static class ImageUrl {
            @JsonProperty("imageUrl") // JSON 내의 실제 키값
            private String imageUrl;        }
    }
}
