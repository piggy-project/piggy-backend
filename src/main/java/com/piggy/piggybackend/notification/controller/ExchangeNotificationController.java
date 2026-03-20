package com.piggy.piggybackend.notification.controller;

import com.piggy.piggybackend.global.util.LoginUser;
import com.piggy.piggybackend.notification.dto.NotificationListResponse;
import com.piggy.piggybackend.notification.dto.NotificationSettingRequest;
import com.piggy.piggybackend.notification.dto.TargetPriceSettingRequest;
import com.piggy.piggybackend.notification.dto.TargetPriceSettingResponse;
import com.piggy.piggybackend.notification.service.ExchangeNotificationService;
import com.piggy.piggybackend.user.entity.User;
import jakarta.validation.Valid;
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
            @LoginUser User user,
            @Valid @RequestBody NotificationSettingRequest request) {

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

        notificationService.updateUserFcmToken(user, tokenRequest.get("fcmToken"));
        return ResponseEntity.ok("FCM 토큰이 업데이트되었습니다.");
    }

    @PostMapping("/target-price")
    public ResponseEntity<String> saveTargetPriceSetting(
            @LoginUser User user,
            @Valid @RequestBody TargetPriceSettingRequest request) {

        notificationService.saveOrUpdateTargetPriceSetting(user.getEmail(), request);
        return ResponseEntity.ok("목표가 알림이 설정되었습니다.");
    }

    @GetMapping("/target-price")
    public ResponseEntity<List<TargetPriceSettingResponse>> getMyTargetPriceSettings(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(notificationService.getMyTargetPriceSettings(email));
    }

    @DeleteMapping("/target-price/{settingId}")
    public ResponseEntity<String> deleteTargetPriceSetting(
            @AuthenticationPrincipal String email,
            @PathVariable Long settingId) {

        notificationService.deleteTargetPriceSetting(email, settingId);
        return ResponseEntity.ok("목표가 알림이 삭제되었습니다.");
    }
}