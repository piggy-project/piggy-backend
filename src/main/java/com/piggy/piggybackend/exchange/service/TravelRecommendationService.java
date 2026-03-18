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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Value("${gemini.api.key}")
    private String geminiApiKey;

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
                    "imageKeyword": "여행지를 나타내는 영어 키워드 2개 (예: hanoi,landscape)"
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

            // ⭐ 서버에서 URL 강제 조립 (죽은 주소 source.unsplash 차단)
            String encodedKeyword = URLEncoder.encode(keyword.replace(" ", ""), StandardCharsets.UTF_8);
            String finalImageUrl = "https://loremflickr.com/800/450/" + encodedKeyword;

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
}