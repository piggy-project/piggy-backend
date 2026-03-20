package com.piggy.piggybackend.notification.service;

import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import com.piggy.piggybackend.exchange.repository.ExchangeFavoriteRepository;
import com.piggy.piggybackend.exchange.repository.ExchangeRateRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.notification.dto.TargetPriceSettingRequest;
import com.piggy.piggybackend.notification.dto.TargetPriceSettingResponse;
import com.piggy.piggybackend.notification.entity.ExchangeTargetPriceSetting;
import com.piggy.piggybackend.notification.repository.ExchangeTargetPriceRepository;
import com.piggy.piggybackend.user.entity.FcmToken;
import com.piggy.piggybackend.user.service.FcmService;
import com.piggy.piggybackend.notification.dto.NotificationListResponse;
import com.piggy.piggybackend.notification.dto.NotificationSettingRequest;
import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.notification.entity.ReferencePeriod;
import com.piggy.piggybackend.notification.repository.ExchangeNotificationRepository;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import com.piggy.piggybackend.user.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeNotificationService {

    private final ExchangeNotificationRepository notificationRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final ExchangeTargetPriceRepository targetPriceRepository;

    private static final String REDIS_LIVE_RATES = "exchange:live";
    private static final int NOTIFICATION_COOLDOWN_HOURS = 6;

    /**
     * FCM 토큰 업데이트 API 용
     */
    @Transactional
    public void updateUserFcmToken(User user, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "FCM 토큰은 필수입니다.");
        }
        fcmTokenService.saveOrUpdateToken(user, fcmToken);
        log.info("[FCM 토큰 업데이트 완료] User: {}", user.getEmail());
    }

    /**
     * 퍼센트 알림 설정 저장 및 수정
     */
    @Transactional
    public void saveOrUpdateSetting(String email, NotificationSettingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.getCurrencyCode() == null || request.getCurrencyCode().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "통화 코드는 필수입니다.");
        }
        if (request.getPeriod() == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "기준 기간은 필수입니다.");
        }
        if (request.getTargetPercent() == null || request.getTargetPercent() <= 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "알림 기준 퍼센트는 0보다 커야 합니다.");
        }

        if (!favoriteRepository.existsByUserAndCurrencyCode(user, request.getCurrencyCode())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "즐겨찾기에 등록된 통화만 알림 설정이 가능합니다.");
        }

        ExchangeNotificationSetting setting = notificationRepository
                .findByUserAndCurrencyCode(user, request.getCurrencyCode())
                .orElse(null);

        if (setting != null) {
            setting.updateSetting(request.getTargetPercent(), request.getPeriod(), request.isActive());
        } else {
            notificationRepository.save(ExchangeNotificationSetting.builder()
                    .user(user)
                    .currencyCode(request.getCurrencyCode())
                    .targetPercent(request.getTargetPercent())
                    .period(request.getPeriod())
                    .isActive(request.isActive())
                    .build());
        }
    }

    /**
     * 퍼센트 알림 체크 및 발송 엔진
     */
    @Transactional
    public void checkAndSendAlerts() {
        List<ExchangeNotificationSetting> allSettings = notificationRepository.findAllByIsActiveTrue();

        Map<Object, Object> liveRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        if (liveRates.isEmpty()) {
            log.warn("[알림 체크 중단] Redis에 실시간 환율 데이터가 없습니다.");
            return;
        }

        for (ExchangeNotificationSetting setting : allSettings) {
            try {
                processIndividualNotification(setting, liveRates);
            } catch (Exception e) {
                log.error("[알림 처리 에러] User: {}, Currency: {}, Error: {}",
                        setting.getUser().getEmail(), setting.getCurrencyCode(), e.getMessage());
            }
        }
    }

    private void processIndividualNotification(ExchangeNotificationSetting setting, Map<Object, Object> liveRates) {
        String code = setting.getCurrencyCode();
        if (!liveRates.containsKey(code)) return;

        double currentRate;
        try {
            currentRate = Double.parseDouble(liveRates.get(code).toString());
        } catch (NumberFormatException e) {
            log.error("[환율 파싱 실패] code: {}, value: {}, error: {}",
                    code, liveRates.get(code), e.getMessage());
            return;
        }

        double baseRate = getBaseRate(code, setting.getPeriod());
        if (baseRate <= 0) return;

        double fluctuation = Math.abs((currentRate - baseRate) / baseRate) * 100;

        if (fluctuation >= setting.getTargetPercent()) {
            if (setting.getLastNotifiedAt() == null ||
                    setting.getLastNotifiedAt().isBefore(LocalDateTime.now().minusHours(NOTIFICATION_COOLDOWN_HOURS))) {

                executePush(setting, currentRate, fluctuation);
                setting.updateLastNotifiedAt();
            }
        }
    }

    private double getBaseRate(String code, ReferencePeriod period) {
        String dbCode = "JPY".equals(code) ? "JPY(100)" : code;
        String targetDate = LocalDate.now().minusDays(period.getDays())
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return exchangeRateRepository.findTopByCurUnitAndSearchDateBeforeOrderBySearchDateDesc(dbCode, targetDate)
                .map(ExchangeRate::getDealBasR)
                .orElse(0.0);
    }

    private void executePush(ExchangeNotificationSetting setting, double currentRate, double fluctuation) {
        String title = String.format("📈 %s 환율 변동 알림", setting.getCurrencyCode());
        String body = String.format("%s 대비 %.1f%% 변동! 현재가: %.2f원",
                setting.getPeriod().getLabel(), fluctuation, currentRate);

        sendPushToAllDevices(setting.getUser(), title, body);

        log.info("[FCM 발송 완료] User: {}, Currency: {}, 기기수: {}",
                setting.getUser().getEmail(), setting.getCurrencyCode(),
                setting.getUser().getFcmTokens().size());
    }

    @Transactional(readOnly = true)
    public List<NotificationListResponse> getMyNotificationSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.findAllByUser(user).stream()
                .map(NotificationListResponse::new)
                .toList();
    }

    @Transactional
    public void deleteNotificationSetting(String email, Long settingId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ExchangeNotificationSetting setting = notificationRepository.findById(settingId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!setting.getUser().equals(user)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "본인의 알림 설정만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(setting);
    }

    /**
     * 목표가 알림 설정 저장/수정
     */
    @Transactional
    public void saveOrUpdateTargetPriceSetting(String email, TargetPriceSettingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.getCurrencyCode() == null || request.getCurrencyCode().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "통화 코드는 필수입니다.");
        }
        if (request.getDirection() == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "알림 방향(ABOVE/BELOW)은 필수입니다.");
        }
        if (request.getTargetPrice() == null || request.getTargetPrice() <= 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "목표 환율은 0보다 커야 합니다.");
        }

        if (!favoriteRepository.existsByUserAndCurrencyCode(user, request.getCurrencyCode())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "즐겨찾기에 등록된 통화만 알림 설정이 가능합니다.");
        }

        ExchangeTargetPriceSetting setting = targetPriceRepository
                .findByUserAndCurrencyCodeAndDirection(user, request.getCurrencyCode(), request.getDirection())
                .orElse(null);

        if (setting != null) {
            setting.updateSetting(request.getTargetPrice(), request.getDirection(), request.isActive());
        } else {
            targetPriceRepository.save(ExchangeTargetPriceSetting.builder()
                    .user(user)
                    .currencyCode(request.getCurrencyCode())
                    .targetPrice(request.getTargetPrice())
                    .direction(request.getDirection())
                    .isActive(request.isActive())
                    .build());
        }
    }

    /**
     * 목표가 알림 체크 및 발송 (스케줄러에서 호출)
     */
    @Transactional
    public void checkAndSendTargetPriceAlerts() {
        List<ExchangeTargetPriceSetting> allSettings = targetPriceRepository.findAllByIsActiveTrue();

        Map<Object, Object> liveRates = redisTemplate.opsForHash().entries(REDIS_LIVE_RATES);
        if (liveRates.isEmpty()) {
            log.warn("[목표가 알림 체크 중단] Redis에 실시간 환율 데이터가 없습니다.");
            return;
        }

        for (ExchangeTargetPriceSetting setting : allSettings) {
            try {
                processTargetPriceNotification(setting, liveRates);
            } catch (Exception e) {
                log.error("[목표가 알림 처리 에러] User: {}, Currency: {}, Error: {}",
                        setting.getUser().getEmail(), setting.getCurrencyCode(), e.getMessage());
            }
        }
    }

    private void processTargetPriceNotification(ExchangeTargetPriceSetting setting, Map<Object, Object> liveRates) {
        String code = setting.getCurrencyCode();
        if (!liveRates.containsKey(code)) return;

        double currentRate;
        try {
            currentRate = Double.parseDouble(liveRates.get(code).toString());
        } catch (NumberFormatException e) {
            log.error("[목표가 환율 파싱 실패] code: {}, value: {}, error: {}",
                    code, liveRates.get(code), e.getMessage());
            return;
        }

        double targetPrice = setting.getTargetPrice();

        boolean triggered = switch (setting.getDirection()) {
            case BELOW -> currentRate <= targetPrice;
            case ABOVE -> currentRate >= targetPrice;
        };

        if (triggered) {
            if (setting.getLastNotifiedAt() == null ||
                    setting.getLastNotifiedAt().isBefore(LocalDateTime.now().minusHours(NOTIFICATION_COOLDOWN_HOURS))) {

                executeTargetPricePush(setting, currentRate);
                setting.updateLastNotifiedAt();
            }
        }
    }

    private void executeTargetPricePush(ExchangeTargetPriceSetting setting, double currentRate) {
        String title = String.format("🎯 %s 목표 환율 도달!", setting.getCurrencyCode());
        String body = String.format("목표가 %.2f원 %s! 현재가: %.2f원",
                setting.getTargetPrice(),
                setting.getDirection().getLabel(),
                currentRate);

        sendPushToAllDevices(setting.getUser(), title, body);

        log.info("[목표가 FCM 발송] User: {}, Currency: {}, 현재가: {}",
                setting.getUser().getEmail(), setting.getCurrencyCode(), currentRate);
    }

    private void sendPushToAllDevices(User user, String title, String body) {
        List<FcmToken> fcmTokens = user.getFcmTokens();
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.warn("[FCM 발송 건너뜀] User: {} 토큰 없음", user.getEmail());
            return;
        }
        for (FcmToken fcmToken : fcmTokens) {
            try {
                fcmService.sendPushNotification(fcmToken.getToken(), title, body);
            } catch (Exception e) {
                log.error("[FCM 개별 발송 실패] User: {}, token: {}, error: {}",
                        user.getEmail(), fcmToken.getToken(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TargetPriceSettingResponse> getMyTargetPriceSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return targetPriceRepository.findAllByUser(user).stream()
                .map(TargetPriceSettingResponse::new)
                .toList();
    }

    @Transactional
    public void deleteTargetPriceSetting(String email, Long settingId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ExchangeTargetPriceSetting setting = targetPriceRepository.findById(settingId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!setting.getUser().equals(user)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "본인의 알림 설정만 삭제할 수 있습니다.");
        }

        targetPriceRepository.delete(setting);
    }
}