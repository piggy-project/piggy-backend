package com.piggy.piggybackend.notification.scheduler;

import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ExchangeNotificationService notificationService;

    @Scheduled(cron = "0 1 * * * *")
    public void runExchangeCheck() {
        notificationService.checkAndSendAlerts();           // 기존 퍼센트 알림
        notificationService.checkAndSendTargetPriceAlerts(); // 신규 목표가 알림
    }


}