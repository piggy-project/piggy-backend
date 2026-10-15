package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.global.redis.service.ApiQuotaService;
import com.piggy.piggybackend.shopping.util.dto.NaverShopDto;
import com.piggy.piggybackend.shopping.util.dto.SerpItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class SerpShopServiceTest {

    private static final String KOREA_RESPONSE = """
            {
              "shopping_results": [
                {
                  "product_id": "123",
                  "title": "에어팟 프로 2",
                  "source": "네이버",
                  "extracted_price": 289000,
                  "link": "https://smartstore.naver.com/item/1",
                  "thumbnail": "https://img/1.jpg"
                },
                {
                  "title": "에어팟 프로 2 (쿠팡)",
                  "source": "쿠팡",
                  "extracted_price": 279000.5,
                  "link": "#",
                  "product_link": "https://www.google.com/shopping/product/2",
                  "thumbnail": "https://img/2.jpg"
                }
              ]
            }
            """;

    private ApiQuotaService apiQuotaService;
    private SerpShopService serpShopService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        apiQuotaService = mock(ApiQuotaService.class);
        when(apiQuotaService.tryAcquireDaily(eq("SERP"), anyInt())).thenReturn(true);

        serpShopService = new SerpShopService(apiQuotaService);
        ReflectionTestUtils.setField(serpShopService, "apiKey", "test-key");
        ReflectionTestUtils.setField(serpShopService, "dailyLimit", 200);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(serpShopService, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void 한국_검색은_gl_kr로_호출하고_결과를_매핑한다() {
        server.expect(requestTo(startsWith("https://serpapi.com/search")))
                .andExpect(queryParam("engine", "google_shopping"))
                .andExpect(queryParam("gl", "kr"))
                .andExpect(queryParam("hl", "ko"))
                .andExpect(queryParam("google_domain", "google.co.kr"))
                .andRespond(withSuccess(KOREA_RESPONSE, MediaType.APPLICATION_JSON));

        List<SerpItemDto> items = serpShopService.getKoreanProducts("에어팟");

        server.verify();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getProductId()).isEqualTo("123");
        assertThat(items.get(0).getPrice()).isEqualTo(289000);
        // link가 "#"이면 product_link로 대체, product_id가 없으면 링크를 ID로 사용
        assertThat(items.get(1).getLink()).isEqualTo("https://www.google.com/shopping/product/2");
        assertThat(items.get(1).getProductId()).isEqualTo("https://www.google.com/shopping/product/2");
    }

    @Test
    void 동남아_검색은_Shopee_Lazada만_남긴다() {
        String response = """
                {"shopping_results": [
                  {"title": "A", "source": "Shopee", "extracted_price": 100, "link": "https://a"},
                  {"title": "B", "source": "Amazon", "extracted_price": 200, "link": "https://b"},
                  {"title": "C", "source": "Lazada TH", "extracted_price": 300, "link": "https://c"}
                ]}
                """;
        server.expect(requestTo(startsWith("https://serpapi.com/search")))
                .andExpect(queryParam("gl", "th"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<SerpItemDto> items = serpShopService.getRegionalProducts("cream", "THB");

        assertThat(items).extracting(SerpItemDto::getTitle).containsExactly("A", "C");
    }

    @Test
    void 일일_한도를_넘으면_SerpAPI를_호출하지_않는다() {
        when(apiQuotaService.tryAcquireDaily(eq("SERP"), anyInt())).thenReturn(false);
        server.expect(never(), requestTo(startsWith("https://serpapi.com/search")));

        List<SerpItemDto> items = serpShopService.getKoreanProducts("에어팟");

        server.verify();
        assertThat(items).isEmpty();
    }

    @Test
    void SerpAPI_오류시_빈_리스트를_반환한다() {
        server.expect(requestTo(startsWith("https://serpapi.com/search")))
                .andRespond(withServerError());

        assertThat(serpShopService.getKoreanProducts("에어팟")).isEmpty();
    }

    @Test
    void NaverShopService는_기존_NaverShopDto_형태로_변환한다() {
        server.expect(requestTo(startsWith("https://serpapi.com/search")))
                .andRespond(withSuccess(KOREA_RESPONSE, MediaType.APPLICATION_JSON));
        NaverShopService naverShopService = new NaverShopService(serpShopService);

        NaverShopDto result = naverShopService.searchItems("에어팟");

        assertThat(result.getData()).hasSize(2);
        NaverShopDto.ProductData first = result.getData().get(0);
        assertThat(first.getId()).isEqualTo("123");
        assertThat(first.getProductName()).isEqualTo("에어팟 프로 2");
        assertThat(first.getSalePrice()).isEqualTo(289000);
        assertThat(first.getMallName()).isEqualTo("네이버");
        assertThat(first.getProductUrl().getPcUrl()).isEqualTo("https://smartstore.naver.com/item/1");
        assertThat(first.getProductUrl().getMobileUrl()).isEqualTo("https://smartstore.naver.com/item/1");
        assertThat(first.getImages().get(0).getImageUrl()).isEqualTo("https://img/1.jpg");
        // 소수점 가격은 int로 버림
        assertThat(result.getData().get(1).getSalePrice()).isEqualTo(279000);
    }
}
