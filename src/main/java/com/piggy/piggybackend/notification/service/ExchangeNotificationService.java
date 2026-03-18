package com.piggy.piggybackend.notification.service;

import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import com.piggy.piggybackend.exchange.repository.ExchangeFavoriteRepository;
import com.piggy.piggybackend.exchange.repository.ExchangeRateRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.service.FcmService;
import com.piggy.piggybackend.notification.dto.NotificationListResponse;
import com.piggy.piggybackend.notification.dto.NotificationSettingRequest;
import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.notification.entity.ReferencePeriod;
import com.piggy.piggybackend.notification.repository.ExchangeNotificationRepository;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
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

    private static final String REDIS_LIVE_RATES = "exchange:live";

    /**
     * FCM 토큰 업데이트 API 용
     */
    @Transactional
    public void updateUserFcmToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // User 엔티티의 fcmToken 필드 업데이트 (Dirty Checking 활용)
        // User 엔티티에 setFcmToken 메서드가 있거나 @Setter가 필요합니다.
        user.updateFcmToken(fcmToken);
        log.info("[FCM 토큰 업데이트] User: {}", email);
    }

    /**
     * 알림 설정 저장 및 수정
     */
    @Transactional
    public void saveOrUpdateSetting(String email, NotificationSettingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!favoriteRepository.existsByUserAndCurrencyCode(user, request.getCurrencyCode())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "즐겨찾기에 등록된 통화만 알림 설정이 가능합니다.");
        }

        if (request.getTargetPercent() <= 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "알림 기준 퍼센트는 0보다 커야 합니다.");
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
     * 알림 체크 및 발송 엔진 (스케줄러에서 이 메서드를 호출하게 설정하세요)
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

        double currentRate = Double.parseDouble(liveRates.get(code).toString());
        double baseRate = getBaseRate(code, setting.getPeriod());

        if (baseRate <= 0) return;

        double fluctuation = Math.abs((currentRate - baseRate) / baseRate) * 100;

        if (fluctuation >= setting.getTargetPercent()) {
            // 도배 방지 (6시간 쿨타임)
            if (setting.getLastNotifiedAt() == null ||
                    setting.getLastNotifiedAt().isBefore(LocalDateTime.now().minusHours(6))) {

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

    /**
     * ⭐ 실제 푸시 발송 실행부
     */
    private void executePush(ExchangeNotificationSetting setting, double currentRate, double fluctuation) {
        String title = String.format("📈 %s 환율 변동 알림", setting.getCurrencyCode());
        String body = String.format("%s 대비 %.1f%% 변동! 현재가: %.2f원",
                setting.getPeriod().getLabel(), fluctuation, currentRate);

        // ✅ User 엔티티에 저장된 실제 fcmToken 필드를 가져옴
        String userToken = setting.getUser().getFcmToken();

        if (userToken != null && !userToken.isEmpty()) {
            fcmService.sendPushNotification(userToken, title, body);
            log.info("[FCM 발송 완료] User: {}, Currency: {}", setting.getUser().getEmail(), setting.getCurrencyCode());
        } else {
            log.warn("[FCM 발송 건너뜀] User: {} 의 토큰이 존재하지 않습니다.", setting.getUser().getEmail());
        }
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
                .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 설정입니다."));

        if (!setting.getUser().equals(user)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "본인의 알림 설정만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(setting);
    }
}