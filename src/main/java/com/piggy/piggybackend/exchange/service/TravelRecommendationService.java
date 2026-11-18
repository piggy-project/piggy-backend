package com.piggy.piggybackend.exchange.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.TravelRecommendationResponseDto;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelRecommendationService {

    private final ExchangeRateService exchangeRateService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String REDIS_KEY = "travel:recommendations";
    private static final String REDIS_ERROR_KEY = "travel:recommendations:error";

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final List<String> TARGET_CURRENCIES = List.of(
            "JPY", "THB", "VND", "PHP", "USD", "EUR", "CNY", "AUD", "GBP"
    );

    private static final String WIKIPEDIA_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800";
    // 위키백과에서 사진을 못 찾았을 때 쓰는 나라별 사진
    private static final Map<String, String> FALLBACK_IMAGES = Map.of(
            "JPY", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800",
            "VND", "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800",
            "THB", "https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=800",
            "EUR", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"
    );

    private static final String REDIS_IMAGE_PREFIX = "travel:image:";
    private static final String IMAGE_USER_AGENT = "PiggyTravelApp/1.0 (graduation project)";
    private static final int MAX_CACHED_IMAGES = 60;
    private final Map<String, TravelImage> imageCache = new ConcurrentHashMap<>();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // 앱이 접근하는 서버 주소 (사진 주소를 만들 때 사용)
    @Value("${app.public-url:https://piggys.mooo.com}")
    private String publicUrl;

    public void clearCache() {
        redisTemplate.delete(REDIS_KEY);
        log.info("[AI 여행 추천] Redis 캐시가 수동으로 초기화되었습니다.");
    }

    @Scheduled(cron = "0 0 * * * *")
    public void refreshRecommendations() {
        log.info("[AI 여행 추천] 캐시 갱신 시작 (스케줄러)");
        try {
            updateCacheWithGemini();
        } catch (Exception e) {
            log.error("[AI 여행 추천] 스케줄러 갱신 실패: {}", e.getMessage());
        }
    }

    public List<TravelRecommendationResponseDto> getRecommendations() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }

            if (Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_ERROR_KEY))) {
                log.warn("[AI 여행 추천] API 에러 쿨타임 중입니다.");
                throw new CustomException(ErrorCode.API_SERVER_ERROR);
            }

            log.info("[AI 여행 추천] 캐시 없음 - Gemini 즉시 호출");
            return updateCacheWithGemini();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI 여행 추천] 조회 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.API_SERVER_ERROR);
        }
    }

    private List<TravelRecommendationResponseDto> updateCacheWithGemini() throws Exception {
        List<TravelRecommendationResponseDto> fresh = fetchFromGemini();
        if (!fresh.isEmpty()) {
            String json = objectMapper.writeValueAsString(fresh);
            redisTemplate.opsForValue().set(REDIS_KEY, json, 65, TimeUnit.MINUTES);
            redisTemplate.delete(REDIS_ERROR_KEY);
            log.info("[AI 여행 추천] Redis 캐시 저장 완료 - {}건", fresh.size());
        }
        return fresh;
    }

    private List<TravelRecommendationResponseDto> fetchFromGemini() throws Exception {
        Map<String, ExchangeDetailResponseDto> rateMap = new HashMap<>();
        StringBuilder rateInfo = new StringBuilder();

        for (String code : TARGET_CURRENCIES) {
            try {
                ExchangeDetailResponseDto detail = exchangeRateService.getExchangeDetail(code);
                rateMap.put(code, detail);
                rateInfo.append(String.format("%s: %.2f원 (전일대비 %.2f%%)\n",
                        code, detail.getCurrentRate(), detail.getPercentChange()));
            } catch (Exception e) {
                log.warn("[AI 여행 추천] {} 환율 조회 실패", code);
            }
        }

        // ⭐ 프롬프트 강화: Gemini가 절대 URL을 생성하지 못하도록 차단
        String prompt = String.format("""
                당신은 가성비 여행 전문가입니다.
                현재 환율 정보:
                %s
                
                위 정보를 바탕으로 지금 한국인이 여행하기 좋은 가성비 여행지 3곳을 추천하세요.
                반드시 아래 JSON 배열 형식으로만 응답하세요. 
                'imageUrl' 필드는 절대 포함하지 마세요. 오직 'imageKeyword'에 관련 키워드만 넣으세요.
                
                [
                  {
                    "destination": "나라명 (예: 베트남)",
                    "location": "도시명 (예: 하노이)",
                    "reason": "추천 이유 (한 문장)",
                    "currencyCode": "통화코드 (예: VND)",
                    "imageKeyword": "도시의 영어 이름 하나, 영어 위키백과 문서 제목 그대로 (예: Hanoi, Da Nang)"
                  }
                ]
                """, rateInfo);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.7)
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            redisTemplate.opsForValue().set(REDIS_ERROR_KEY, "ERROR", 1, TimeUnit.MINUTES);
            throw new CustomException(ErrorCode.API_SERVER_ERROR);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        content = content.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]") + 1;
        List<JsonNode> geminiResults = objectMapper.readValue(content.substring(start, end), new TypeReference<>() {});

        return geminiResults.stream().map(item -> {
            String code = item.path("currencyCode").asText();
            String location = item.path("location").asText();
            // Gemini가 준 키워드 사용 (없으면 도시명)
            String keyword = item.path("imageKeyword").asText().isEmpty() ? location : item.path("imageKeyword").asText();

            ExchangeDetailResponseDto detail = rateMap.get(code);

            // 이미지 주소는 서버에서 만든다 (Gemini가 지어낸 주소, 죽은 이미지 서비스 차단)
            String finalImageUrl = resolveImageUrl(keyword, code);

            return TravelRecommendationResponseDto.builder()
                    .destination(item.path("destination").asText())
                    .location(location)
                    .reason(item.path("reason").asText())
                    .currencyCode(code)
                    .currencyName(code)
                    .exchangeRate(detail != null ? detail.getCurrentRate() : 0.0)
                    .percentChange(detail != null ? detail.getPercentChange() : 0.0)
                    .imageUrl(finalImageUrl) // 서버에서 만든 '진짜' 주소 삽입
                    .build();
        }).toList();
    }

    /**
     * 앱에 내려준 여행지 사진을 서버가 대신 받아 준다.
     * 위키미디어는 안드로이드 앱의 기본 요청(okhttp)을 403으로 막기 때문에 앱이 직접 받지 않는다.
     * Redis에 등록된 사진만 내려주므로 임의의 주소를 받아 오는 통로로는 쓸 수 없다.
     */
    public TravelImage loadImage(String slug) {
        if (slug == null || !slug.matches("[a-z0-9-]{1,80}")) {
            return null;
        }
        TravelImage cached = imageCache.get(slug);
        if (cached != null) {
            return cached;
        }
        try {
            String source = redisTemplate.opsForValue().get(REDIS_IMAGE_PREFIX + slug);
            if (source == null) {
                return null;
            }
            URI uri = URI.create(source);
            if (uri.getHost() == null || !uri.getHost().endsWith(".wikimedia.org")) {
                return null;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", IMAGE_USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                log.warn("[AI 여행 추천] 사진 다운로드 실패 - slug: {}, status: {}", slug, response.statusCode());
                return null;
            }
            String contentType = source.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            TravelImage image = new TravelImage(response.body(), contentType);
            if (imageCache.size() >= MAX_CACHED_IMAGES) {
                imageCache.clear();
            }
            imageCache.put(slug, image);
            return image;
        } catch (Exception e) {
            log.warn("[AI 여행 추천] 사진 다운로드 에러 - slug: {}, error: {}", slug, e.getMessage());
            return null;
        }
    }

    public record TravelImage(byte[] bytes, String contentType) {}

    /**
     * 도시 이름으로 영어 위키백과의 대표 사진을 찾아, 서버를 거쳐 받는 주소로 돌려준다.
     * 찾지 못하면 통화(나라)별로 정해 둔 사진을 쓴다.
     */
    private String resolveImageUrl(String keyword, String currencyCode) {
        try {
            // "hanoi,landscape"처럼 여러 단어가 오면 첫 단어(도시명)만 쓴다
            String title = keyword.split(",")[0].trim().replace(' ', '_');
            if (!title.isEmpty()) {
                String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(WIKIPEDIA_SUMMARY_URL + encodedTitle))
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", IMAGE_USER_AGENT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String thumbnail = objectMapper.readTree(response.body())
                            .path("thumbnail").path("source").asText("");
                    if (!thumbnail.isEmpty()) {
                        // 위키미디어가 허용하는 표준 폭(960px)으로 바꾸고 추적용 쿼리는 뗀다
                        String withoutQuery = thumbnail.split("\\?")[0];
                        String source = withoutQuery.replaceFirst("/\\d+px-", "/960px-");

                        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
                        if (!slug.isEmpty() && slug.length() <= 80) {
                            redisTemplate.opsForValue().set(REDIS_IMAGE_PREFIX + slug, source, 7, TimeUnit.DAYS);
                            return publicUrl + "/api/exchange/travel-image/" + slug;
                        }
                    }
                }
                log.warn("[AI 여행 추천] 위키백과 이미지 없음 - keyword: {}, status: {}", keyword, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("[AI 여행 추천] 이미지 조회 실패 - keyword: {}, error: {}", keyword, e.getMessage());
        }
        return FALLBACK_IMAGES.getOrDefault(currencyCode, DEFAULT_IMAGE);
    }
}