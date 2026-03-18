package com.piggy.piggybackend.notification.scheduler;

import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ExchangeNotificationService notificationService;

    // 1시간(3,600,000ms)마다 환율 변동을 체크하여 푸시 발송
    @Scheduled(fixedRate = 3600000)
    public void runExchangeCheck() {
        notificationService.checkAndSendAlerts();
    }
}