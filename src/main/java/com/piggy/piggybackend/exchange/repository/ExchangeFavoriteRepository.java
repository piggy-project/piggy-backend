package com.piggy.piggybackend.exchange.repository;

import com.piggy.piggybackend.exchange.entity.ExchangeFavorite;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExchangeFavoriteRepository extends JpaRepository<ExchangeFavorite, Long> {

    List<ExchangeFavorite> findAllByUser(User user);

    Optional<ExchangeFavorite> findByUserAndCurrencyCode(User user, String currencyCode);

    boolean existsByUserAndCurrencyCode(User user, String currencyCode);
    // 최저가 알림을 켜둔 즐겨찾기 목록 조회 (N+1 방지를 위해 fetch join 권장)
    @Query("SELECT f FROM ExchangeFavorite f JOIN FETCH f.user u LEFT JOIN FETCH u.fcmTokens WHERE f.isLowestAlertOn = true")
    List<ExchangeFavorite> findAllByIsLowestAlertOnTrue();

    // 급변동 알림을 켜둔 즐겨찾기 목록 조회
    @Query("SELECT f FROM ExchangeFavorite f JOIN FETCH f.user u LEFT JOIN FETCH u.fcmTokens WHERE f.isVolatilityAlertOn = true")
    List<ExchangeFavorite> findAllByIsVolatilityAlertOnTrue();
}