package com.piggy.piggybackend.exchange.repository;

import com.piggy.piggybackend.exchange.entity.ExchangeFavorite;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeFavoriteRepository extends JpaRepository<ExchangeFavorite, Long> {

    List<ExchangeFavorite> findAllByUser(User user);

    Optional<ExchangeFavorite> findByUserAndCurrencyCode(User user, String currencyCode);

    boolean existsByUserAndCurrencyCode(User user, String currencyCode);
}