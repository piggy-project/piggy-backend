package com.piggy.piggybackend.user.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.piggy.piggybackend.user.entity.FcmToken;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 특정 사용자의 모든 등록 기기에 푸시 알림 발송 및 만료 토큰 정리
     */
    @Transactional
    public void sendPushToUserDevices(User user, String title, String body) {
        List<FcmToken> fcmTokens = user.getFcmTokens();
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.warn("[FCM 발송 건너뜀] User: {} 토큰 없음", user.getEmail());
            return;
        }

        for (FcmToken fcmToken : fcmTokens) {
            try {
                boolean isValid = sendPushNotification(fcmToken.getToken(), title, body);
                // 만료된 토큰이면 DB에서 삭제
                if (!isValid) {
                    fcmTokenRepository.delete(fcmToken);
                    log.info("[FCM 좀비 토큰 삭제] User: {}, token: {}", user.getEmail(), mask(fcmToken.getToken()));
                }
            } catch (Exception e) {
                log.error("[FCM 개별 발송 실패] User: {}, token: {}, error: {}",
                        user.getEmail(), mask(fcmToken.getToken()), e.getMessage());
            }
        }
    }

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
                log.warn("[FCM] 만료된 토큰 감지: {}", mask(token));
                return false;
            }
            // 네트워크 오류 등 다른 이유 → 토큰은 유효하므로 삭제 안 함
            log.error("[FCM] 발송 에러: {}", e.getMessage());
            return true;
        }
    }

    // 로그에 토큰 전체가 남지 않도록 끝 6자리만 표시
    private String mask(String token) {
        if (token == null || token.length() <= 6) return "******";
        return "..." + token.substring(token.length() - 6);
    }
}
