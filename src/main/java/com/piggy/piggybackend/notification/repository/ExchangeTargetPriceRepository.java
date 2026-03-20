package com.piggy.piggybackend.notification.repository;

import com.piggy.piggybackend.notification.entity.ExchangeTargetPriceSetting;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExchangeTargetPriceRepository extends JpaRepository<ExchangeTargetPriceSetting, Long> {

    // N+1 방지용 JOIN FETCH
    @Query("SELECT DISTINCT t FROM ExchangeTargetPriceSetting t " +
            "JOIN FETCH t.user u " +
            "LEFT JOIN FETCH u.fcmTokens " +
            "WHERE t.isActive = true")
    List<ExchangeTargetPriceSetting> findAllByIsActiveTrue();

    // 유저 + 통화 + 방향 조합으로 중복 방지 (USD 이하 알림은 1개만 허용)
    Optional<ExchangeTargetPriceSetting> findByUserAndCurrencyCodeAndDirection(
            User user, String currencyCode, com.piggy.piggybackend.notification.entity.PriceDirection direction);

    List<ExchangeTargetPriceSetting> findAllByUser(User user);
}