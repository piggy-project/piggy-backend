package com.piggy.piggybackend.notification.controller;

import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.notification.dto.NotificationListResponse;
import com.piggy.piggybackend.notification.dto.NotificationSettingRequest;
import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import com.piggy.piggybackend.user.entity.User;
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

    // 1. 알림 설정 저장 (유저 엔티티 바로 주입)
    @PostMapping("/settings")
    public ResponseEntity<String> updateNotificationSetting(
            @LoginUser User user,
            @RequestBody NotificationSettingRequest request) {

        notificationService.saveOrUpdateSetting(user.getEmail(), request);
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
            @LoginUser User user,
            @RequestBody Map<String, String> tokenRequest) {

        String token = tokenRequest.get("fcmToken");
        notificationService.updateUserFcmToken(user, token);
        return ResponseEntity.ok("FCM 토큰이 업데이트되었습니다.");
    }
}