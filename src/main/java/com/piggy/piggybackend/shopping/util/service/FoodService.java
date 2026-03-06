package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.FoodResponse;
import com.piggy.piggybackend.shopping.util.entity.RegionalFood;
import com.piggy.piggybackend.shopping.util.repository.RegionalFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodService {
    private final RegionalFoodRepository foodRepository;

    @Transactional(readOnly = true)
    public List<FoodResponse> getRandomFiveFoods(String countryCode) {
        // 1. 해당 국가의 전체 음식(100개)을 가져옵니다.
        List<RegionalFood> allFoods =
                foodRepository.findByCountryCodeOrderByRankAsc(countryCode);

        // 2. 리스트를 무작위로 섞습니다.
        Collections.shuffle(allFoods);

        // 3. 섞인 리스트에서 최대 5개만 추출하여 DTO로 변환합니다.
        return allFoods.stream()
                .limit(5)
                .map(FoodResponse::new)
                .collect(Collectors.toList());
    }
    //10개국 통화 코드 -> 국가 코드 변환 로직
    private String convertCurrencyToCountry(String currencyCode) {
        if (currencyCode == null) return "JPN"; // 기본값 설정

        return switch (currencyCode.toUpperCase()) {
            case "JPY" -> "JPN"; // 일본
            case "USD" -> "USA"; // 미국
            case "VND" -> "VNM"; // 베트남
            case "PHP" -> "PHL"; // 필리핀
            case "THB" -> "THA"; // 태국
            case "CNY" -> "CHN"; // 중국
            case "AUD" -> "AUS"; // 호주
            case "GBP" -> "GBR"; // 영국
            case "EUR" -> "DEU"; // 독일 (프랑스와 통화가 같으므로 기본 국가 설정 필요)
            case "FRA_EUR" -> "FRA";
            default -> "JPN"; // 매칭되는 게 없으면 일본 데이터 반환
        };
    }
}