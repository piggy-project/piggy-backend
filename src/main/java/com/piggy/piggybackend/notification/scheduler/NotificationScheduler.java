package com.piggy.piggybackend.notification.scheduler;

import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import com.piggy.piggybackend.notification.service.SpecialNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ExchangeNotificationService notificationService;
    private final SpecialNotificationService specialNotificationService;

    @Scheduled(cron = "0 1 * * * *")
    public void runExchangeCheck() {
        notificationService.checkAndSendAlerts();           // 기존 퍼센트 알림
        notificationService.checkAndSendTargetPriceAlerts(); // 신규 목표가 알림
    }

    // NotificationScheduler.java 내부
    @Scheduled(cron = "0 5 * * * *") // 매시 5분 실행 (API 갱신이 0분에 일어나므로 데이터 안정화 후 실행)
    public void runSpecialAlertsCheck() {
        specialNotificationService.checkAndSendLowestPriceAlert();
        specialNotificationService.checkAndSendVolatilityAlert();
    }


}