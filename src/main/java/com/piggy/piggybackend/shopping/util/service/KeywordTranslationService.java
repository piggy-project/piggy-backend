package com.piggy.piggybackend.shopping.util.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 한글 검색어를 해외 쇼핑몰이 이해하는 언어로 바꿔 준다.
 * Rakuten 등은 한글 키워드에 결과를 주지 않기 때문에, 해외 검색 직전에만 사용한다.
 * 번역에 실패하면 원래 검색어를 그대로 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordTranslationService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String CACHE_PREFIX = "keyword_translation:";
    private static final int MAX_KEYWORD_LENGTH = 100;

    // 통화(=검색 대상 쇼핑몰)별 검색 언어
    private static final Map<String, String> MARKET_LANGUAGE = Map.of(
            "JPY", "Japanese",
            "USD", "English",
            "EUR", "English",
            "GBP", "English",
            "AUD", "English",
            "PHP", "English",
            "THB", "Thai",
            "VND", "Vietnamese",
            "CNY", "Simplified Chinese"
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String toMarketLanguage(String query, String currencyCode) {
        String language = MARKET_LANGUAGE.get(currencyCode);
        if (language == null || query == null || query.isBlank() || !containsHangul(query)) {
            return query;
        }

        String keyword = query.trim();
        String cacheKey = CACHE_PREFIX + language + ":" + keyword;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }

            String translated = requestTranslation(keyword, language);
            if (translated == null || translated.isBlank()) {
                return query;
            }

            redisTemplate.opsForValue().set(cacheKey, translated, 30, TimeUnit.DAYS);
            log.info("[검색어 번역] {} -> {} ({})", keyword, translated, language);
            return translated;

        } catch (Exception e) {
            log.warn("[검색어 번역] 실패, 원문으로 검색합니다 - query: {}, error: {}", keyword, e.getMessage());
            return query;
        }
    }

    private String requestTranslation(String keyword, String language) throws Exception {
        String prompt = String.format("""
                Translate the Korean shopping search keyword below into %s, the way a shopper would type it \
                into an online store in that language. Use the common local product or brand name.
                Reply with the translated keyword only: one line, no quotes, no explanation.

                Keyword: %s
                """, language, keyword);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "thinkingConfig", Map.of("thinkingBudget", 0)
                )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + geminiApiKey))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("[검색어 번역] Gemini 응답 코드 {}", response.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");

        // 첫 줄만 사용하고 따옴표를 걷어낸다
        String firstLine = text.strip().lines().findFirst().orElse("").strip();
        String cleaned = firstLine.replaceAll("^[\"'`「『]+|[\"'`」』]+$", "").strip();
        if (cleaned.isEmpty() || cleaned.length() > MAX_KEYWORD_LENGTH) {
            return null;
        }
        return cleaned;
    }

    private boolean containsHangul(String text) {
        return text.codePoints().anyMatch(cp ->
                (cp >= 0xAC00 && cp <= 0xD7A3)      // 완성형 한글
                        || (cp >= 0x1100 && cp <= 0x11FF)   // 자모
                        || (cp >= 0x3130 && cp <= 0x318F)); // 호환 자모
    }
}
