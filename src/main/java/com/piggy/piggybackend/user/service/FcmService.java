package com.piggy.piggybackend.user.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    /**
     * 반환값: true = 발송 성공, false = 토큰 만료 (삭제 필요)
     */
    public boolean sendPushNotification(String token, String title, String body) {
        if (token == null || token.isEmpty()) {
            log.warn("[FCM] 토큰이 없어 알림 발송을 취소합니다.");
            return false;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 발송 성공: {}", response);
            return true;
        } catch (FirebaseMessagingException e) {
            // 만료된 토큰 → 삭제 필요 신호
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("[FCM] 만료된 토큰 감지: {}", token);
                return false;
            }
            // 네트워크 오류 등 다른 이유 → 토큰은 유효하므로 삭제 안 함
            log.error("[FCM] 발송 에러: {}", e.getMessage());
            return true;
        }
    }
}