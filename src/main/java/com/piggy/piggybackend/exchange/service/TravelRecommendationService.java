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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private static final String REDIS_ERROR_KEY = "travel:recommendations:error"; // 에러 발생 시 짧은 캐싱용

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final List<String> TARGET_CURRENCIES = List.of(
            "JPY", "THB", "VND", "PHP", "USD", "EUR","CNY","AUD","GBP"
    );

    @Value("${gemini.api.key}")
    private String geminiApiKey;

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
            // 1. 정상 캐시 확인
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }

            // 2. 에러 쿨타임(Short-lived Cache) 확인: 너무 잦은 API 재시도 방지
            if (Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_ERROR_KEY))) {
                log.warn("[AI 여행 추천] API 에러 쿨타임 중입니다. 잠시 후 다시 시도해주세요.");
                throw new CustomException(ErrorCode.API_SERVER_ERROR); // 프론트에 500 또는 적절한 에러 반환
            }

            log.info("[AI 여행 추천] 캐시 없음 - Gemini 즉시 호출");
            return updateCacheWithGemini();

        } catch (CustomException e) {
            throw e; // 이미 처리된 커스텀 예외는 그대로 던짐
        } catch (Exception e) {
            log.error("[AI 여행 추천] 조회 로직 중 알 수 없는 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.API_SERVER_ERROR);
        }
    }

    private List<TravelRecommendationResponseDto> updateCacheWithGemini() throws Exception {
        List<TravelRecommendationResponseDto> fresh = fetchFromGemini();
        if (!fresh.isEmpty()) {
            String json = objectMapper.writeValueAsString(fresh);
            redisTemplate.opsForValue().set(REDIS_KEY, json, 65, TimeUnit.MINUTES);
            redisTemplate.delete(REDIS_ERROR_KEY); // 성공 시 에러 캐시 지움
            log.info("[AI 여행 추천] Redis 캐시 저장 완료 - {}건", fresh.size());
        }
        return fresh;
    }

    private List<TravelRecommendationResponseDto> fetchFromGemini() throws Exception {

        // ⭐ 개선 1: DB 조회를 한 번만 하고 Map에 저장 (최대 9번 -> 딱 6번으로 고정)
        Map<String, ExchangeDetailResponseDto> rateMap = new HashMap<>();
        StringBuilder rateInfo = new StringBuilder();

        for (String code : TARGET_CURRENCIES) {
            try {
                ExchangeDetailResponseDto detail = exchangeRateService.getExchangeDetail(code);
                rateMap.put(code, detail);
                rateInfo.append(String.format("%s: %.2f원 (전일대비 %.2f%%)\n",
                        code, detail.getCurrentRate(), detail.getPercentChange()));
            } catch (Exception e) {
                log.warn("[AI 여행 추천] {} 환율 조회 실패, 스킵", code);
            }
        }

        String prompt = String.format("""
                당신은 가성비 여행 전문가입니다.
                아래는 현재 실시간 환율 정보입니다:
                
                %s
                
                위 환율 정보를 바탕으로 지금 한국인이 여행하기 가성비 좋은 여행지 3곳을 추천해주세요.
                또한 시기에 맞춰서 그 시기에 유명한 여행지도 고려해서 추천해주세요.
                
                반드시 아래 JSON 배열 형식으로만 응답하세요. 다른 텍스트나 마크다운 코드블록 없이 순수 JSON만 응답하세요:
                [
                  {
                    "destination": "여행지명 (예: 일본 도쿄)",
                    "reason": "추천 이유 (1문장에서 2문장 문장은 "가성비 여행 추천!!", "겨울에는 후쿠오카!"이런식으로)",
                    "currencyCode": "통화코드 (예: JPY)"
                  }
                ]
                """, rateInfo);

        String requestBody = objectMapper.writeValueAsString(
                new HashMap<String, Object>() {{
                    put("contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ));
                    put("generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 2048
                    ));
                }}
        );

        String url = GEMINI_API_URL + "?key=" + geminiApiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ⭐ 개선 2: 명확한 에러 로깅 및 "에러 쿨타임 캐시" 적용
        if (response.statusCode() != 200) {
            log.error("[AI 여행 추천] Gemini API 응답 에러 (상태코드: {}): {}", response.statusCode(), response.body());

            // 429 에러 등 발생 시, 1분 동안은 Gemini API를 다시 부르지 않도록 차단 캐시 설정
            redisTemplate.opsForValue().set(REDIS_ERROR_KEY, "ERROR", 1, TimeUnit.MINUTES);
            throw new CustomException(ErrorCode.API_SERVER_ERROR);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        content = content.replaceAll("```json", "").replaceAll("```", "").trim();

        int start = content.indexOf("[");
        int end = content.lastIndexOf("]") + 1;
        if (start == -1 || end == 0) {
            log.error("[AI 여행 추천] Gemini 응답 파싱 실패: {}", content);
            throw new CustomException(ErrorCode.API_SERVER_ERROR);
        }

        String jsonArray = content.substring(start, end);
        List<JsonNode> geminiResults = objectMapper.readValue(jsonArray, new TypeReference<>() {});

        // ⭐ 개선 3: 미리 만들어둔 rateMap에서 꺼내 쓰기 (DB 조회 안 함!)
        return geminiResults.stream().map(item -> {
            String code = item.path("currencyCode").asText();
            ExchangeDetailResponseDto detail = rateMap.get(code); // Map에서 꺼냄

            double rate = detail != null ? detail.getCurrentRate() : 0.0;
            double percentChange = detail != null ? detail.getPercentChange() : 0.0;

            return TravelRecommendationResponseDto.builder()
                    .destination(item.path("destination").asText())
                    .reason(item.path("reason").asText())
                    .currencyCode(code)
                    .currencyName(code)
                    .exchangeRate(rate)
                    .percentChange(percentChange)
                    .build();
        }).toList();
    }
}

