package com.piggy.piggybackend.notification.repository;

import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExchangeNotificationRepository extends JpaRepository<ExchangeNotificationSetting, Long> {

    // 알림이 켜져 있는 모든 설정 조회 (스케줄러에서 사용)
    List<ExchangeNotificationSetting> findAllByIsActiveTrue();

    // 특정 유저의 특정 통화 알림 설정 조회
    Optional<ExchangeNotificationSetting> findByUserAndCurrencyCode(User user, String currencyCode);

    // 유저의 전체 알림 설정 목록
    List<ExchangeNotificationSetting> findAllByUser(User user);
}