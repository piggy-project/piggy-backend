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
            List.of("여행", "추천", "가볼만한", "국가", "나라", "도시", "여행지");

    private static final List<String> UI_KEYWORDS =
            List.of("게시판", "커뮤니티", "사용법", "어떻게 사용", "메뉴", "기능",
                    "마이페이지", "마이 탭", "프로필", "환율 조회", "쇼핑", "탐색",
                    "로그인", "회원가입", "어디 있", "어디에 있", "어디서", "어떻게 가",
                    "탭", "화면", "홈", "알림", "글쓰기", "스캔", "즐겨찾기",
                    "회원탈퇴", "비밀번호 변경", "닉네임", "관심 상품");

    // ─────────────────────────────────────────
    // PIGGY 앱 UI 안내 컨텍스트
    // ─────────────────────────────────────────
    private static final String UI_GUIDE_CONTEXT = """

            [PIGGY 앱 UI 안내 정보]
            앱 이름: PIGGY (환율·여행·쇼핑·커뮤니티 통합 앱)

            [하단 탭 메뉴 - 왼쪽부터 순서대로]
            1. 탐색 탭 (route: /exchange)
               - 오늘의 추천 여행지 배너
               - 해시태그별 여행지 랭킹: #물가저렴 #가성비갑 #쇼핑천국 #힐링휴양 #액티비티 #가족여행
               - 통화별 실시간 환율 목록 (독일유로, 프랑스유로, 미국달러, 호주달러, 영국파운드, 베트남동, 필리핀페소, 태국바트, 일본엔화)
               - 통화 클릭 시 환율 상세(그래프, 1일/1개월/3개월/1년, 일별 시세) (route: /exchange/currency)

            2. 쇼핑 탭 (route: /shopping)
               - 국가별 탭 (베트남, 미국, 일본 등)
               - 현지 추천 음식 및 직구 추천템 목록
               - 우측 하단 카메라(📷) 버튼: 상품 가격 OCR 스캔 (route: /shopping/scanner)

            3. 홈 탭 (route: /) - 가운데 집 모양 아이콘
               - 환전 타이밍 안내 메시지 + 돼지 캐릭터
               - 환율 그래프 요약

            4. 커뮤니티 탭 (route: /community)
               - 상단 검색창, 우측 상단 🔔 알림 버튼
               - 게시판 종류: 자유게시판 / 환율게시판(나라 필터: 대한민국·독일·미국·베트남·영국·일본·태국·프랑스·필리핀·호주) / 여행게시판
               - 핫 게시글: 핑크 배경으로 상단 표시
               - 우측 하단 "+ 글쓰기" 버튼으로 게시글 작성 (route: /community/write)

            5. 마이 탭 (route: /mypage)
               - 프로필 (닉네임, 프로필 사진)
               - 내 관심 상품 목록
               - 내가 쓴 게시물 / 댓글 / 좋아요 탭

            [주요 기능 위치]
            - 환율 즐겨찾기: 탐색 탭 → 통화 클릭 → 상단 ♥ 버튼
            - 환율 알림 설정: 탐색 탭 → 통화 클릭 → 상단 🔔 버튼
            - 상품 가격 스캔(OCR): 쇼핑 탭 → 우측 하단 📷 카메라 버튼
            - 게시글 작성: 커뮤니티 탭 → 우측 하단 "+ 글쓰기" 버튼
            - 알림 목록: 각 탭 우측 상단 🔔 버튼
            - 내정보 수정(닉네임·사진·비밀번호): 마이 탭 → 프로필 옆 > 버튼 (route: /mypage/edit)
            - 비밀번호 재설정: 마이 탭 → 내정보 수정 → 비밀번호 재설정
            - 로그아웃·회원탈퇴: 마이 탭 → 내정보 수정 화면 하단
            - 로그인/회원가입: 앱 시작화면 → 카카오(노란)·구글(G)·네이버(N)·이메일 로그인 선택 (route: /login)
            """;

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
            String extraContext = buildExtraContext(userQuestion);
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
            String extraContext = buildExtraContext(userQuestion);
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

        String normalized = question
                .replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]", "")
                .replaceAll("(해줘|해줄래|해주세요|좀|줘|주랑|주세요|알려줘|알려주세요|추천해|부탁해|추천좀|있어|없어|뭐야|뭔데|어때)$", "")
                .trim();

        return CHAT_CACHE_PREFIX + normalized;

    }

    private boolean isTravelRecommendation(String question) {
        return TRAVEL_KEYWORDS.stream().anyMatch(question::contains);
    }

    private boolean isUiQuestion(String question) {
        return UI_KEYWORDS.stream().anyMatch(question::contains);
    }

    private String buildExtraContext(String question) {
        String context = "";
        if (isUiQuestion(question)) {
            context += UI_GUIDE_CONTEXT;
        }
        if (isTravelRecommendation(question)) {
            context += fetchExchangeRatesFromRedis();
        }
        return context;
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
                당신은 PIGGY 앱의 친절한 도우미입니다. 아래 두 가지 역할만 수행합니다.

                1. [앱 UI 안내] 사용자가 특정 기능이나 화면 위치를 물어보면, 제공된 UI 안내 정보를 바탕으로 어디에 있는지 친절하게 설명하세요.
                   navigationLinks는 아래 5개 메인 탭 경로만 사용하세요. 서브 기능(OCR, 글쓰기 등)은 answer 텍스트로만 위치를 안내하세요.
                   사용 가능한 navigationLinks 경로:
                   - { "label": "메인 바로가기", "route": "/" }
                   - { "label": "환율 바로가기", "route": "/exchange" }
                   - { "label": "커뮤니티 바로가기", "route": "/community" }
                   - { "label": "쇼핑 바로가기", "route": "/shopping" }
                   - { "label": "마이페이지 바로가기", "route": "/mypage" }
                2. [여행/음식 전문가] 여행지 추천, 현지 음식, 환율 정보를 바탕으로 답변하세요. 환율이 유리한 나라와 현재 날씨/축제로 유명한 여행지를 우선 추천하세요.

                [중요] 위 두 가지 주제(앱 사용법, 여행/환율/현지 음식)와 관련 없는 질문은 절대 답변하지 마세요.
                관련 없는 질문의 예: 날씨, 요리 레시피, 연예인, 스포츠, 정치, 수학 문제, 코딩 질문 등.
                이런 경우 answer에 "저는 PIGGY 앱 안내와 여행/환율 관련 질문만 답변할 수 있어요 😊 여행지 추천이나 앱 사용법을 물어봐 주세요!"라고만 반환하고, navigationLinks와 suggestedQuestions는 빈 배열로 두세요.

                반드시 아래 JSON 형식으로만 응답하세요. 마크다운이나 다른 텍스트는 절대 포함하지 마세요.
                UI 관련 질문이면 navigationLinks를 채우고, 아니면 빈 배열로 두세요.
                {
                  "answer": "질문에 대한 친절한 답변 (3줄 이내)",
                  "navigationLinks": [
                    { "label": "버튼에 표시될 텍스트", "route": "위 5개 경로 중 하나" }
                  ],
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