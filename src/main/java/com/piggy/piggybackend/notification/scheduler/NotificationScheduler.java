package com.piggy.piggybackend.notification.scheduler;

import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ExchangeNotificationService notificationService;

    // 매시 1분 0초에 실행 (0초 1분 *시 *일 *월 *요일)
    @Scheduled(cron = "0 1 * * * *")
    public void runExchangeCheck() {
        notificationService.checkAndSendAlerts();
    }
}