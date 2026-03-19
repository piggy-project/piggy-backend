package com.piggy.piggybackend.notification.repository;

import com.piggy.piggybackend.notification.entity.ExchangeNotificationSetting;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ExchangeNotificationRepository extends JpaRepository<ExchangeNotificationSetting, Long> {

    // JOIN FETCH를 적용하여 N+1 문제 해결
    @Query("SELECT DISTINCT n FROM ExchangeNotificationSetting n " +
            "JOIN FETCH n.user u " +
            "LEFT JOIN FETCH u.fcmTokens " +
            "WHERE n.isActive = true")
    List<ExchangeNotificationSetting> findAllByIsActiveTrue();

    // 특정 유저의 특정 통화 알림 설정 조회
    Optional<ExchangeNotificationSetting> findByUserAndCurrencyCode(User user, String currencyCode);

    // 유저의 전체 알림 설정 목록
    List<ExchangeNotificationSetting> findAllByUser(User user);
}