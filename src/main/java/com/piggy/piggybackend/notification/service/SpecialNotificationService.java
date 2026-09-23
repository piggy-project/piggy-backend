package com.piggy.piggybackend.notification.service;

import com.piggy.piggybackend.exchange.entity.ExchangeFavorite;
import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import com.piggy.piggybackend.exchange.repository.ExchangeFavoriteRepository;
import com.piggy.piggybackend.exchange.repository.ExchangeRateRepository;
import com.piggy.piggybackend.notification.entity.NotificationType;
import com.piggy.piggybackend.user.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialNotificationService {

    private final ExchangeRateRepository rateRepository;
    private final ExchangeFavoriteRepository favoriteRepository;
    private final StringRedisTemplate redisTemplate;
    private final FcmService fcmService;
    private final UserNotificationService userNotificationService;

    private static final String REDIS_LIVE_RATES = "exchange:live";
    private static final String REDIS_LIVE_RATES_HOUR_PREFIX = "exchange:live:h:";

    /**
     * 1. 3개월 최저가 알림 (매시간 체크)
     */
    @Transactional
    public void checkAndSendLowestPriceAlert() {
        List<ExchangeFavorite> targets = favoriteRepository.findAllByIsLowestAlertOnTrue();
        if (targets.isEmpty()) return;

        Map<Object, Object> liveRates = new HashMap<>(redisTemplate.opsForHash().entries(REDIS_LIVE_RATES));
        if (liveRates.isEmpty()) {
            liveRates.put("__DB_FALLBACK__", "0");
        }
        String threeMonthsAgo = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (ExchangeFavorite fav : targets) {
            String code = fav.getCurrencyCode();
            String dbCode = "JPY".equals(code) ? "JPY(100)" : code;

            // Redis 쿨다운 체크 (하루 1번만 발송)
            String cooldownKey = "alert:lowest:" + code + ":" + fav.getUser().getId() + ":" + todayStr;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) continue;

            putLatestDbRateIfAbsent(code, liveRates);
            if (liveRates.containsKey(code)) {
                double currentRate = Double.parseDouble(liveRates.get(code).toString());

                // 3개월 내 최저가 조회
                ExchangeRate lowestRecord = rateRepository
                        .findTopByCurUnitAndSearchDateGreaterThanEqualOrderByDealBasRAsc(dbCode, threeMonthsAgo)
                        .orElse(null);

                // 현재가가 3개월 내 최저가보다 낮거나 같으면 알림 발송
                if (lowestRecord != null && currentRate <= lowestRecord.getDealBasR()) {
                    String title = "📉 " + code + " 최저가 도달!";
                    String body = String.format("지금이 기회예요! %s가 최근 3개월 중 가장 낮은 %.2f원에 진입했습니다.", code, currentRate);

                    userNotificationService.save(fav.getUser(), NotificationType.LOWEST_PRICE, title, body, code);
                    fcmService.sendPushToUserDevices(fav.getUser(), title, body);
                    log.info("[최저가 알림 발송] User: {}, Currency: {}, Rate: {}", fav.getUser().getEmail(), code, currentRate);

                    // 발송 후 오늘 하루 동안 해당 유저/통화 알림 방지 (24시간 TTL)
                    redisTemplate.opsForValue().set(cooldownKey, "done", 24, TimeUnit.HOURS);
                }
            }
        }
    }

    /**
     * 2. 급등/급락 변동성 알림 (매시간 체크, 6시간 전 대비 1.5% 이상 변동 시)
     */
    private void putLatestDbRateIfAbsent(String code, Map<Object, Object> liveRates) {
        if (liveRates.containsKey(code)) {
            return;
        }

        String dbCode = "JPY".equals(code) ? "JPY(100)" : code;
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        rateRepository.findTopByCurUnitAndSearchDateBeforeOrderBySearchDateDesc(dbCode, tomorrow)
                .map(ExchangeRate::getDealBasR)
                .ifPresent(rate -> {
                    liveRates.put(code, rate.toString());
                    log.info("[특수 알림 환율 DB fallback] code: {}, rate: {}", code, rate);
                });
    }

    @Transactional
    public void checkAndSendVolatilityAlert() {
        List<ExchangeFavorite> targets = favoriteRepository.findAllByIsVolatilityAlertOnTrue();
        if (targets.isEmpty()) return;

        Map<Object, Object> liveRates = new HashMap<>(redisTemplate.opsForHash().entries(REDIS_LIVE_RATES));
        
        // 6시간 전 시간 계산
        int targetHour = (LocalDateTime.now().getHour() - 6 + 24) % 24;
        String targetHourStr = String.format("%02d", targetHour);
        Map<Object, Object> agoRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES_HOUR_PREFIX + targetHourStr);

        if (liveRates.isEmpty() || agoRates.isEmpty()) {
            log.debug("[변동성 체크 건너뜀] 데이터 부족 (현재: {}, 6시간전: {})", liveRates.size(), agoRates.size());
            return;
        }

        for (ExchangeFavorite fav : targets) {
            String code = fav.getCurrencyCode();

            // Redis 쿨다운 체크 (6시간 1번 발송)
            String cooldownKey = "alert:volatility:" + code + ":" + fav.getUser().getId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) continue;

            if (liveRates.containsKey(code) && agoRates.containsKey(code)) {
                double current = Double.parseDouble(liveRates.get(code).toString());
                double ago = Double.parseDouble(agoRates.get(code).toString());

                double fluctuation = ((current - ago) / ago) * 100;

                // 1.5% 이상 급등(+) 또는 급락(-)
                if (Math.abs(fluctuation) >= 1.5) {
                    String direction = fluctuation > 0 ? "급등" : "급락";
                    String title = "🚨 " + code + " 변동 주의!";
                    String body = String.format("지난 6시간 동안 %s 환율이 %.1f%% %s했습니다. (현재가: %.2f원)",
                            code, Math.abs(fluctuation), direction, current);

                    userNotificationService.save(fav.getUser(), NotificationType.VOLATILITY, title, body, code);
                    fcmService.sendPushToUserDevices(fav.getUser(), title, body);
                    log.info("[변동성 알림 발송] User: {}, Currency: {}, Fluctuation: {}%", fav.getUser().getEmail(), code, fluctuation);

                    // 6시간 쿨다운 설정
                    redisTemplate.opsForValue().set(cooldownKey, "done", 6, TimeUnit.HOURS);
                }
            }
        }
    }
}
