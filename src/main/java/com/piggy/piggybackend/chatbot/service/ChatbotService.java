package com.piggy.piggybackend.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.chatbot.dto.ChatbotResponse;
import com.piggy.piggybackend.chatbot.dto.SuggestedOnlyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
public class ChatbotService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String CHAT_CACHE_PREFIX = "chat:cache:";

    private static final List<String> TRAVEL_KEYWORDS =
            List.of("여행", "추천", "가볼만한", "어디", "국가", "나라", "도시", "여행지");

    // ─────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────

    /** 일반 질문 → answer + suggestedQuestions 전체 반환 */
    public ChatbotResponse getChatResponse(String userQuestion) {
        String normalizedKey = buildCacheKey(userQuestion);

        try {
            String cachedJson = redisTemplate.opsForValue().get(normalizedKey);
            if (cachedJson != null) {
                log.info("[챗봇] 캐시 히트: {}", userQuestion);
                return objectMapper.readValue(cachedJson, ChatbotResponse.class);
            }

            log.info("[챗봇] 캐시 미스 - Gemini 호출: {}", userQuestion);
            String extraContext = isTravelRecommendation(userQuestion) ? fetchExchangeRatesFromRedis() : "";
            ChatbotResponse response = fetchFromGemini(userQuestion, extraContext);

            redisTemplate.opsForValue().set(
                    normalizedKey,
                    objectMapper.writeValueAsString(response),
                    1, TimeUnit.HOURS
            );

            return response;

        } catch (Exception e) {
            log.error("[챗봇] getChatResponse 에러: ", e);
            return new ChatbotResponse("죄송합니다. 잠시 후 다시 시도해주세요.", List.of());
        }
    }

    /** 후속질문 클릭 시 → preview는 이미 프론트에 표시됐으므로 suggestedQuestions만 반환 */
    public SuggestedOnlyResponse getSuggestedOnly(String userQuestion) {
        String normalizedKey = buildCacheKey(userQuestion);

        try {
            // ask와 같은 키 공유 → 이미 캐싱됐으면 AI 호출 없이 바로 반환
            String cachedJson = redisTemplate.opsForValue().get(normalizedKey);
            if (cachedJson != null) {
                log.info("[챗봇] 후속질문 캐시 히트: {}", userQuestion);
                ChatbotResponse cached = objectMapper.readValue(cachedJson, ChatbotResponse.class);
                return new SuggestedOnlyResponse(cached.getSuggestedQuestions());
            }

            log.info("[챗봇] 후속질문 캐시 미스 - Gemini 호출: {}", userQuestion);
            String extraContext = isTravelRecommendation(userQuestion) ? fetchExchangeRatesFromRedis() : "";
            ChatbotResponse response = fetchFromGemini(userQuestion, extraContext);

            // 전체 캐싱 (이후 /ask로 같은 질문 와도 히트)
            redisTemplate.opsForValue().set(
                    normalizedKey,
                    objectMapper.writeValueAsString(response),
                    1, TimeUnit.HOURS
            );

            return new SuggestedOnlyResponse(response.getSuggestedQuestions());

        } catch (Exception e) {
            log.error("[챗봇] getSuggestedOnly 에러: ", e);
            return new SuggestedOnlyResponse(List.of());
        }
    }

    // ─────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────

    private String buildCacheKey(String question) {
        return CHAT_CACHE_PREFIX + question.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]", "");
    }

    private boolean isTravelRecommendation(String question) {
        return TRAVEL_KEYWORDS.stream().anyMatch(question::contains);
    }

    private String fetchExchangeRatesFromRedis() {
        try {
            List<String> currencies = List.of("JPY", "THB", "VND", "PHP", "MYR", "USD", "EUR");
            StringBuilder sb = new StringBuilder("\n\n[현재 환율 정보 (KRW 기준)]\n");
            for (String currency : currencies) {
                String rate = redisTemplate.opsForValue().get("exchange:" + currency);
                if (rate != null) {
                    sb.append(String.format("- %s: %s원\n", currency, rate));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[챗봇] Redis 환율 조회 실패, 컨텍스트 없이 진행: {}", e.getMessage());
            return "";
        }
    }

    private ChatbotResponse fetchFromGemini(String userQuestion, String extraContext) throws Exception {
        String systemPrompt = """
                당신은 세계 여행 및 현지 음식 전문가입니다.
                맛집/추천 요청 시 해당 지역의 특산물, 유명 음식, 가성비 식당 위주로 답변하세요.
                여행지 추천 시 환율이 유리한 나라, 그리고 현재 시기에 날씨/축제 등으로 유명한 여행지를 우선 추천하세요.

                반드시 아래 JSON 형식으로만 응답하세요. 마크다운이나 다른 텍스트는 절대 포함하지 마세요.
                {
                  "answer": "질문에 대한 친절한 답변 (3줄 이내)",
                  "suggestedQuestions": [
                    { "question": "예상 후속 질문 1", "preview": "한 줄 답변 요약" },
                    { "question": "예상 후속 질문 2", "preview": "한 줄 답변 요약" }
                  ]
                }
                """;

        String fullPrompt = systemPrompt + extraContext + "\n질문: " + userQuestion;

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", fullPrompt)))
        ));

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new RuntimeException("Gemini API Key가 설정되지 않았습니다.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBodyMap)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[챗봇] API 호출 실패 바디: {}", response.body());
            throw new RuntimeException("Gemini API Error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String rawText = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // ```json ... ``` 마크다운 제거
        String cleanJson = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();

        return objectMapper.readValue(cleanJson, ChatbotResponse.class);
    }
}