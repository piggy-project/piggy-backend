package com.piggy.piggybackend.shopping.util.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JacksonXmlRootElement(localName = "ProductSearchResponse")
public class ElevenShopDto {

    @JacksonXmlProperty(localName = "Products")
    private Products products;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Products {
        @JacksonXmlProperty(localName = "Product")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ElevenProduct> productList;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ElevenProduct {
        @JacksonXmlProperty(localName = "ProductCode")
        private String productCode;

        @JacksonXmlProperty(localName = "ProductName")
        private String productName;

        @JacksonXmlProperty(localName = "ProductPrice")
        private Integer productPrice;

        @JacksonXmlProperty(localName = "ProductImage300")
        private String productImage;

        @JacksonXmlProperty(localName = "DetailPageUrl")
        private String detailPageUrl;
    }
}