package com.piggy.piggybackend.exchange.service;

import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeFavoriteResponseDto;
import com.piggy.piggybackend.exchange.entity.ExchangeFavorite;
import com.piggy.piggybackend.exchange.repository.ExchangeFavoriteRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeFavoriteService {

    private final ExchangeFavoriteRepository favoriteRepository;
    private final ExchangeRateService exchangeRateService;
    private final UserRepository userRepository;

    private static final Map<String, String> CURRENCY_NAME_MAP = Map.of(
            "JPY", "일본 엔",
            "CNY", "중국 위안",
            "USD", "미국 달러",
            "VND", "베트남 동",
            "PHP", "필리핀 페소",
            "THB", "태국 바트",
            "EUR", "유럽연합 유로",
            "AUD", "호주 달러",
            "GBP", "영국 파운드"
    );

    @Transactional
    public boolean toggleFavorite(String email, String currencyCode) {
        User user = getUser(email);

        return favoriteRepository.findByUserAndCurrencyCode(user, currencyCode)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    log.info("[즐겨찾기] 제거 - user: {}, currency: {}", email, currencyCode);
                    return false;
                })
                .orElseGet(() -> {
                    favoriteRepository.save(ExchangeFavorite.builder()
                            .user(user)
                            .currencyCode(currencyCode)
                            .build());
                    log.info("[즐겨찾기] 추가 - user: {}, currency: {}", email, currencyCode);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public List<ExchangeFavoriteResponseDto> getFavorites(String email) {
        User user = getUser(email);
        List<ExchangeFavorite> favorites = favoriteRepository.findAllByUser(user);

        List<String> codes = favorites.stream()
                .map(ExchangeFavorite::getCurrencyCode)
                .toList();

        Map<String, ExchangeDetailResponseDto> detailMap = exchangeRateService.getExchangeDetailsBatch(codes);

        return favorites.stream()
                .map(favorite -> {
                    String code = favorite.getCurrencyCode();
                    ExchangeDetailResponseDto detail = detailMap.get(code);
                    if (detail == null) {
                        log.warn("[즐겨찾기] {} 환율 조회 실패", code);
                        return null;
                    }
                    return ExchangeFavoriteResponseDto.builder()
                            .currencyCode(code)
                            .currencyName(CURRENCY_NAME_MAP.getOrDefault(code, code))
                            .currentRate(detail.getCurrentRate())
                            .change(detail.getChange())
                            .percentChange(detail.getPercentChange())
                            .high(detail.getHigh())
                            .low(detail.getLow())
                            .build();
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String email, String currencyCode) {
        User user = getUser(email);
        return favoriteRepository.existsByUserAndCurrencyCode(user, currencyCode);
    }

    // 에러 처리 로직
    private User getUser(String email) {
        if (email == null || email.equals("anonymousUser")) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}