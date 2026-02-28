package com.piggy.piggybackend.exchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeHistoryResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeRateResponseDto;
import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import com.piggy.piggybackend.exchange.repository.ExchangeRateRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String REDIS_LIVE_RATES = "exchange:live";
    private static final String REDIS_HIGH_RATES = "exchange:high";
    private static final String REDIS_LOW_RATES = "exchange:low";
    private static final String REDIS_UPDATE_TIME = "exchange:time";

    private static final Map<String, String> currencyMap = Map.ofEntries(
            Map.entry("USD", "미국 달러"), Map.entry("EUR", "유럽연합 유로"),
            Map.entry("JPY", "일본 엔"), Map.entry("CNY", "중국 위안"),
            Map.entry("HKD", "홍콩 달러"), Map.entry("GBP", "영국 파운드"),
            Map.entry("CHF", "스위스 프랑"), Map.entry("CAD", "캐나다 달러"),
            Map.entry("AUD", "호주 달러"), Map.entry("NZD", "뉴질랜드 달러"),
            Map.entry("SGD", "싱가포르 달러"), Map.entry("THB", "태국 바트"),
            Map.entry("VND", "베트남 동"), Map.entry("PHP", "필리핀 페소"),
            Map.entry("MYR", "말레이시아 링깃"), Map.entry("TWD", "대만 달러"),
            Map.entry("SAR", "사우디 리얄"), Map.entry("AED", "아랍에미리트 디르함"),
            Map.entry("KWD", "쿠웨이트 디나르"), Map.entry("BHD", "바레인 디나르"),
            Map.entry("IDR", "인도네시아 루피아"), Map.entry("SEK", "스웨덴 크로나")
    );

    @Value("${EXCHANGE_RATE_API_KEY}")
    private String erApiKey;

    private final String ER_API_URL = "https://api.currencybeacon.com/v1/latest";

    // 1. [실시간 캐싱] Redis로 저장
    @Scheduled(cron = "0 0 * * * *")
    public void updateLiveRatesCache() {
        try {
            String url = ER_API_URL + "?api_key=" + erApiKey;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("외부 환율 API 에러: HTTP Status {}", response.statusCode());
                return; // 스케줄러이므로 예외를 던지기보다 로그를 남기고 종료
            }
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            Map<String, Object> responseMap = (Map<String, Object>) body.get("response");

            if (responseMap == null || !responseMap.containsKey("rates")) {
                log.error("외부 환율 API 응답 구조가 올바르지 않습니다.");
                return;
            }

            Map<String, Object> rates = (Map<String, Object>) responseMap.get("rates");
            Double krwInBase = Double.parseDouble(String.valueOf(rates.get("KRW")));

            currencyMap.keySet().forEach(code -> {
                Object rateValue = rates.get(code);
                if (rateValue != null) {
                    Double rateInBase = Double.parseDouble(String.valueOf(rateValue));
                    Double krwRate = krwInBase / rateInBase;
                    if ("JPY".equals(code)) krwRate *= 100;

                    double roundedRate = Math.round(krwRate * 100) / 100.0;
                    // Redis Hash에 실시간 환율 저장
                    redisTemplate.opsForHash().put(REDIS_LIVE_RATES, code, String.valueOf(roundedRate));
                    updateHighLowInRedis(code, roundedRate);
                }
            });

            String nowTime = LocalTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"));
            redisTemplate.opsForValue().set(REDIS_UPDATE_TIME, nowTime);
            log.info("Redis 환율 업데이트 완료: {}", nowTime);

        } catch (Exception e) {
            log.error("Redis 환율 갱신 중 예외 발생: {}", e.getMessage());
        }
    }

    // 고가/저가 업데이트
    private void updateHighLowInRedis(String code, double currentRate) {
        Object highObj = redisTemplate.opsForHash().get(REDIS_HIGH_RATES, code);
        Object lowObj = redisTemplate.opsForHash().get(REDIS_LOW_RATES, code);

        double lastHigh = highObj != null ? Double.parseDouble(highObj.toString()) : currentRate;
        double lastLow = lowObj != null ? Double.parseDouble(lowObj.toString()) : currentRate;

        if (currentRate >= lastHigh)
            redisTemplate.opsForHash().put(REDIS_HIGH_RATES, code, String.valueOf(currentRate));
        if (currentRate <= lastLow) redisTemplate.opsForHash().put(REDIS_LOW_RATES, code, String.valueOf(currentRate));
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyHighLow() {
        redisTemplate.delete(REDIS_HIGH_RATES);
        redisTemplate.delete(REDIS_LOW_RATES);
        log.info("자정 경과: Redis 당일 고가/저가 초기화 완료");
    }

    //Redis 기반으로 마감 환율 저장
    @Scheduled(cron = "0 10 2 * * MON-SAT")
    @Transactional
    public void saveDailyClosingRates() {
        Map<Object, Object> rates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);

        if (rates.isEmpty()) {
            updateLiveRatesCache();
            rates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        }

        String yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<ExchangeRate> closingRates = rates.entrySet().stream()
                .map(entry -> {
                    String code = entry.getKey().toString();
                    return ExchangeRate.builder()
                            .curUnit("JPY".equals(code) ? "JPY(100)" : code)
                            .curNm(currencyMap.get(code))
                            .dealBasR(Double.parseDouble(entry.getValue().toString()))
                            .searchDate(yesterday)
                            .build();
                })
                .collect(Collectors.toList());

        repository.saveAll(closingRates);
        log.info(" [{}] Redis 데이터를 기반으로 전일 마감 환율 DB 저장 완료", yesterday);
    }

    // 2. 전체 환율 조회 (Redis 기반)
    public List<ExchangeRateResponseDto> getLatestRates() {
        Map<Object, Object> rates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);

        // 캐시가 비어있으면 즉시 갱신 시도
        if (rates.isEmpty()) {
            updateLiveRatesCache();
            rates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        }

        // 갱신 후에도 비어있다면 외부 API 서버 혹은 Redis 문제
        if (rates.isEmpty()) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }

        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
        String updateTime = redisTemplate.opsForValue().get(REDIS_UPDATE_TIME);

        return rates.entrySet().stream()
                .map(e -> ExchangeRateResponseDto.builder()
                        .curUnit(e.getKey().toString())
                        .curNm(currencyMap.get(e.getKey().toString()))
                        .rate(Double.parseDouble(e.getValue().toString()))
                        .date(today)
                        .time(updateTime)
                        .build())
                .collect(Collectors.toList());
    }

    // 3. 상세 환율 조회 (Redis 기반)
    @Transactional(readOnly = true)
    public ExchangeDetailResponseDto getExchangeDetail(String code) {
        // 지원하지 않는 통화 코드 요청 시 에러 처리
        if (!currencyMap.containsKey(code)) {
            throw new CustomException(ErrorCode.GRAPH_NOT_SUPPORTED);
        }

        Object rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
        if (rateObj == null) {
            updateLiveRatesCache();
            rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
        }
        // 여전히 데이터가 없으면 데이터 부재 에러
        if (rateObj == null) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }

        double currentRate = rateObj != null ? Double.parseDouble(rateObj.toString()) : 0.0;
        String todayStr = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dbCode = "JPY".equals(code) ? "JPY(100)" : code;

        Optional<ExchangeRate> yesterdayOpt = repository.findTopByCurUnitAndSearchDateBeforeOrderBySearchDateDesc(dbCode, todayStr);

        double change = 0.0;
        double percentChange = 0.0;
        if (yesterdayOpt.isPresent()) {
            double yesterdayRate = yesterdayOpt.get().getDealBasR();
            if (yesterdayRate > 0) {
                change = currentRate - yesterdayRate;
                percentChange = (change / yesterdayRate) * 100;
            }
        }

        Object high = redisTemplate.opsForHash().get(REDIS_HIGH_RATES, code);
        Object low = redisTemplate.opsForHash().get(REDIS_LOW_RATES, code);

        return ExchangeDetailResponseDto.builder()
                .code(code)
                .currentRate(currentRate)
                .change(Math.round(change * 100) / 100.0)
                .percentChange(Math.round(percentChange * 100) / 100.0)
                .high(high != null ? Double.parseDouble(high.toString()) : currentRate)
                .low(low != null ? Double.parseDouble(low.toString()) : currentRate)
                .build();
    }

    // 전일 마감가 DB 조회 로직
    public List<ExchangeHistoryResponseDto> getExchangeHistory(String code) {
        String dbCode = "JPY".equals(code) ? "JPY(100)" : code;
        List<ExchangeRate> history = repository.findAllByCurUnitOrderBySearchDateAsc(dbCode);
        return history.stream()
                .map(record -> {
                    String rawDate = record.getSearchDate();
                    String formattedDate = rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);
                    return new ExchangeHistoryResponseDto(formattedDate, record.getDealBasR());
                })
                .collect(Collectors.toList());
    }
}