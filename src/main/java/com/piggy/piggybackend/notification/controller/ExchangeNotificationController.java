package com.piggy.piggybackend.notification.controller;

import com.piggy.piggybackend.notification.dto.NotificationListResponse;
import com.piggy.piggybackend.notification.dto.NotificationSettingRequest;
import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class ExchangeNotificationController {

    private final ExchangeNotificationService notificationService;

    @PostMapping("/settings")
    public ResponseEntity<String> updateNotificationSetting(
            @AuthenticationPrincipal String email, // 인증 정보에서 이메일 추출
            @RequestBody NotificationSettingRequest request) {

        notificationService.saveOrUpdateSetting(email, request);
        return ResponseEntity.ok("알림 설정이 저장되었습니다.");
    }

    @GetMapping("/settings")
    public ResponseEntity<List<NotificationListResponse>> getMySettings(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(notificationService.getMyNotificationSettings(email));
    }

    @DeleteMapping("/settings/{settingId}")
    public ResponseEntity<String> deleteSetting(
            @AuthenticationPrincipal String email,
            @PathVariable Long settingId) {

        notificationService.deleteNotificationSetting(email, settingId);
        return ResponseEntity.ok("알림 설정이 삭제되었습니다.");
    }

    @PostMapping("/token")
    public ResponseEntity<String> updateFcmToken(
            @AuthenticationPrincipal String email,
            @RequestBody Map<String, String> tokenRequest) {

        String token = tokenRequest.get("fcmToken");
        notificationService.updateUserFcmToken(email, token);
        return ResponseEntity.ok("FCM 토큰이 업데이트되었습니다.");
    }



}