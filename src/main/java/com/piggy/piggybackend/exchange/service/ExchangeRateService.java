package com.piggy.piggybackend.exchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeHistoryResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeRankingResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeTimingResponseDto;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private static final String REDIS_LIVE_RATES_HOUR_PREFIX = "exchange:live:h:";
    private static final String REDIS_UPDATE_TIME = "exchange:time";
    private static final String REDIS_HIGH_RATES = "exchange:high";
    private static final String REDIS_LOW_RATES = "exchange:low";
    private static final String REDIS_TIMING_PREFIX = "timing:";
    private static final String REDIS_YESTERDAY_RATES = "exchange:yesterday";

    private static final Map<String, String> currencyMap = Map.ofEntries(
            Map.entry("JPY", "일본 엔"),
            Map.entry("CNY", "중국 위안"),
            Map.entry("USD", "미국 달러"),
            Map.entry("VND", "베트남 동"),
            Map.entry("PHP", "필리핀 페소"),
            Map.entry("THB", "태국 바트"),
            Map.entry("EUR", "유럽연합 유로"), // 독일, 프랑스 공통
            Map.entry("AUD", "호주 달러"),
            Map.entry("GBP", "영국 파운드")
    );

    @Value("${EXCHANGE_RATE_API_KEY}")
    private String erApiKey;

    private final String ER_API_URL = "https://api.currencybeacon.com/v1/latest";

    // 1. [실시간 캐싱] Redis로 저장
    @Scheduled(cron = "0 0 * * * *")
    public void updateLiveRatesCache() {
        try {
            // [추가] 새로운 데이터를 받아오기 전에 현재 데이터를 해당 시간의 스냅샷으로 저장 (25시간 유지)
            Map<Object, Object> currentRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
            if (!currentRates.isEmpty()) {
                String currentHour = LocalTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH"));
                String historyKey = REDIS_LIVE_RATES_HOUR_PREFIX + currentHour;
                redisTemplate.opsForHash().putAll(historyKey, currentRates);
                redisTemplate.expire(historyKey, 25, java.util.concurrent.TimeUnit.HOURS);
                log.info("Redis {}시 환율 스냅샷 저장 완료", currentHour);
            }

            String url = ER_API_URL + "?api_key=" + erApiKey;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("외부 환율 API 에러: HTTP Status {}", response.statusCode());
                return;
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
                    redisTemplate.opsForHash().put(REDIS_LIVE_RATES, code, String.valueOf(roundedRate));
                    updateHighLowInRedis(code, roundedRate);
                }
            });

            String nowTime = LocalTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"));
            redisTemplate.opsForValue().set(REDIS_UPDATE_TIME, nowTime);
            log.info("Redis 환율 업데이트 완료: {}", nowTime);

            updateTimingCache();

        } catch (Exception e) {
            log.error("Redis 환율 갱신 중 예외 발생: {}", e.getMessage());
        }
    }

    public void updateTimingCache() {
        String startDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(90)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        currencyMap.keySet().forEach(code -> {
            try {
                Object rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
                if (rateObj == null) return;
                double currentRate = Double.parseDouble(rateObj.toString());

                String dbCode = "JPY".equals(code) ? "JPY(100)" : code;
                List<ExchangeRate> history = repository.findRecentRates(dbCode, startDate);
                if (history.isEmpty()) return;

                long higherCount = history.stream()
                        .filter(e -> e.getDealBasR() > currentRate)
                        .count();
                int percentile = (int) Math.round((double) higherCount / history.size() * 100);

                redisTemplate.opsForValue().set(
                        REDIS_TIMING_PREFIX + code,
                        String.valueOf(percentile),
                        1, java.util.concurrent.TimeUnit.HOURS
                );
            } catch (Exception e) {
                log.warn("[타이밍] {} 계산 실패: {}", code, e.getMessage());
            }
        });
        log.info("환율 타이밍 캐시 업데이트 완료");
    }

    // 고가/저가 업데이트
    private void updateHighLowInRedis(String code, double currentRate) {
        Object highObj = redisTemplate.opsForHash().get(REDIS_HIGH_RATES, code);
        Object lowObj = redisTemplate.opsForHash().get(REDIS_LOW_RATES, code);

        double lastHigh = highObj != null ? Double.parseDouble(highObj.toString()) : currentRate;
        double lastLow = lowObj != null ? Double.parseDouble(lowObj.toString()) : currentRate;

        if (currentRate >= lastHigh)
            redisTemplate.opsForHash().put(REDIS_HIGH_RATES, code, String.valueOf(currentRate));
        if (currentRate <= lastLow)
            redisTemplate.opsForHash().put(REDIS_LOW_RATES, code, String.valueOf(currentRate));
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyHighLow() {
        redisTemplate.delete(REDIS_HIGH_RATES);
        redisTemplate.delete(REDIS_LOW_RATES);
        log.info("자정 경과: Redis 당일 고가/저가 초기화 완료");
    }

    // Redis 기반으로 마감 환율 저장
    @Scheduled(cron = "0 10 2 * * *")
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
        log.info("[{}] Redis 데이터를 기반으로 전일 마감 환율 DB 저장 완료", yesterday);

        // 전일 마감가 Redis 캐시 갱신 (매 요청 DB 조회 방지)
        Map<String, String> yesterdayCache = rates.entrySet().stream()
                .filter(e -> currencyMap.containsKey(e.getKey().toString()))
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));
        redisTemplate.opsForHash().putAll(REDIS_YESTERDAY_RATES, yesterdayCache);
        redisTemplate.expire(REDIS_YESTERDAY_RATES, 25, java.util.concurrent.TimeUnit.HOURS);
        log.info("전일 마감 환율 Redis 캐시 갱신 완료");
    }

    // 상세 환율 조회 (Redis 기반)
    @Transactional(readOnly = true)
    public ExchangeDetailResponseDto getExchangeDetail(String code) {
        if (!currencyMap.containsKey(code)) {
            throw new CustomException(ErrorCode.GRAPH_NOT_SUPPORTED);
        }

        Object rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
        if (rateObj == null) {
            updateLiveRatesCache();
            rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
        }
        if (rateObj == null) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }

        double currentRate = Double.parseDouble(rateObj.toString());
        String todayStr = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dbCode = "JPY".equals(code) ? "JPY(100)" : code;

        // 전일 마감가 Redis 캐시 우선 조회, 없으면 DB 폴백
        Object yesterdayObj = redisTemplate.opsForHash().get(REDIS_YESTERDAY_RATES, code);
        double yesterdayRate;
        if (yesterdayObj != null) {
            yesterdayRate = Double.parseDouble(yesterdayObj.toString());
        } else {
            Optional<ExchangeRate> yesterdayOpt = repository.findTopByCurUnitAndSearchDateBeforeOrderBySearchDateDesc(dbCode, todayStr);
            yesterdayRate = yesterdayOpt.map(ExchangeRate::getDealBasR).orElse(currentRate);
        }

        double change = 0.0;
        double percentChange = 0.0;
        if (yesterdayRate > 0) {
            change = currentRate - yesterdayRate;
            percentChange = (change / yesterdayRate) * 100;
        }

        Object high = redisTemplate.opsForHash().get(REDIS_HIGH_RATES, code);
        Object low = redisTemplate.opsForHash().get(REDIS_LOW_RATES, code);

        // 타이밍(백분위) 정보 추가
        String timingCached = redisTemplate.opsForValue().get(REDIS_TIMING_PREFIX + code);
        Integer timing = timingCached != null ? Integer.parseInt(timingCached) : null;

        return ExchangeDetailResponseDto.builder()
                .code(code)
                .currentRate(currentRate)
                .change(Math.round(change * 100) / 100.0)
                .percentChange(Math.round(percentChange * 100) / 100.0)
                .high(high != null ? Double.parseDouble(high.toString()) : currentRate)
                .low(low != null ? Double.parseDouble(low.toString()) : currentRate)
                .timing(timing)
                .build();
    }

    // 일일 시간별(09:00 ~ 22:00) 환율 내역 조회
    public List<ExchangeHistoryResponseDto> getDailyHourlyHistory(String code) {
        List<ExchangeHistoryResponseDto> hourlyHistory = new ArrayList<>();

        // 09시부터 22시까지 Redis에 저장된 스냅샷을 순회
        for (int h = 9; h <= 22; h++) {
            String hourStr = String.format("%02d", h); // "09", "10" ...
            String historyKey = REDIS_LIVE_RATES_HOUR_PREFIX + hourStr;

            Object rateObj = redisTemplate.opsForHash().get(historyKey, code);

            if (rateObj != null) {
                // 프론트엔드 기대 형식인 "HH:00"으로 시간 포맷팅
                hourlyHistory.add(new ExchangeHistoryResponseDto(
                        hourStr + ":00",
                        Double.parseDouble(rateObj.toString())
                ));
            }
        }

        // 만약 오늘 데이터가 하나도 없다면(오전 9시 이전 등), 현재가라도 하나 넣어줌
        if (hourlyHistory.isEmpty()) {
            Object currentRate = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, code);
            if (currentRate != null) {
                String nowHour = LocalTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:00"));
                hourlyHistory.add(new ExchangeHistoryResponseDto(nowHour, Double.parseDouble(currentRate.toString())));
            }
        }

        return hourlyHistory;
    }

    // 환율 리스트 조회 및 정렬
    // sort: RISE(급상승), FALL(급하락), HIGH(환율 높은순), LOW(환율 낮은순)
    @Transactional(readOnly = true)
    public List<ExchangeRankingResponseDto> getExchangeRanking(String sort) {

        // Redis에서 현재 환율 전체 조회
        Map<Object, Object> liveRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        if (liveRates.isEmpty()) {
            updateLiveRatesCache();
            liveRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        }
        if (liveRates.isEmpty()) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:00"));
        String todayStr = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 전일 마감가 Redis 캐시 우선 조회, 없으면 DB 폴백
        Map<Object, Object> yesterdayRedis = redisTemplate.opsForHash().entries(REDIS_YESTERDAY_RATES);
        Map<String, Double> yesterdayRateMap;
        if (!yesterdayRedis.isEmpty()) {
            yesterdayRateMap = yesterdayRedis.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> Double.parseDouble(e.getValue().toString())
                    ));
        } else {
            List<ExchangeRate> yesterdayRates = repository.findLatestBeforeDate(todayStr);
            yesterdayRateMap = yesterdayRates.stream()
                    .collect(Collectors.toMap(
                            e -> "JPY(100)".equals(e.getCurUnit()) ? "JPY" : e.getCurUnit(),
                            ExchangeRate::getDealBasR,
                            (a, b) -> a
                    ));
            // DB 조회 결과를 캐시에 저장
            Map<String, String> toCache = yesterdayRateMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
            if (!toCache.isEmpty()) {
                redisTemplate.opsForHash().putAll(REDIS_YESTERDAY_RATES, toCache);
                redisTemplate.expire(REDIS_YESTERDAY_RATES, 25, java.util.concurrent.TimeUnit.HOURS);
            }
        }

        // DTO 변환
        List<ExchangeRankingResponseDto> result = liveRates.entrySet().stream()
                .filter(e -> currencyMap.containsKey(e.getKey().toString()))
                .map(e -> {
                    String code = e.getKey().toString();
                    double currentRate = Double.parseDouble(e.getValue().toString());
                    double yesterdayRate = yesterdayRateMap.getOrDefault(code, currentRate);

                    double change = 0.0;
                    double percentChange = 0.0;
                    if (yesterdayRate > 0) {
                        change = currentRate - yesterdayRate;
                        percentChange = (change / yesterdayRate) * 100;
                    }

                    // 타이밍(백분위) 정보 추가
                    String timingCached = redisTemplate.opsForValue().get(REDIS_TIMING_PREFIX + code);
                    Integer timing = timingCached != null ? Integer.parseInt(timingCached) : null;

                    return ExchangeRankingResponseDto.builder()
                            .curUnit(code)
                            .curNm(currencyMap.get(code))
                            .rate(currentRate)
                            .change(Math.round(change * 100) / 100.0)
                            .percentChange(Math.round(percentChange * 100) / 100.0)
                            .time(baseTime)
                            .timing(timing)
                            .build();
                })
                .collect(Collectors.toList());

        // 정렬
        Comparator<ExchangeRankingResponseDto> comparator = switch (sort.toUpperCase()) {
            case "RISE" -> Comparator.comparingDouble(ExchangeRankingResponseDto::getPercentChange).reversed();
            case "FALL" -> Comparator.comparingDouble(ExchangeRankingResponseDto::getPercentChange);
            case "HIGH" -> Comparator.comparingDouble(ExchangeRankingResponseDto::getRate).reversed();
            case "LOW"  -> Comparator.comparingDouble(ExchangeRankingResponseDto::getRate);
            default -> throw new CustomException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 정렬 기준입니다. (RISE/FALL/HIGH/LOW)");
        };

        return result.stream().sorted(comparator).collect(Collectors.toList());
    }

    // 환율 타이밍 조회
    public ExchangeTimingResponseDto getExchangeTiming(String code) {
        if (!currencyMap.containsKey(code)) {
            throw new CustomException(ErrorCode.GRAPH_NOT_SUPPORTED);
        }

        String cached = redisTemplate.opsForValue().get(REDIS_TIMING_PREFIX + code);
        if (cached != null) {
            return new ExchangeTimingResponseDto(code, Integer.parseInt(cached));
        }

        // 캐시 미스 시 직접 계산
        updateTimingCache();
        String recalculated = redisTemplate.opsForValue().get(REDIS_TIMING_PREFIX + code);
        if (recalculated == null) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }
        return new ExchangeTimingResponseDto(code, Integer.parseInt(recalculated));
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
