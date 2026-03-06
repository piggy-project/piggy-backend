package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.FoodResponse;
import com.piggy.piggybackend.shopping.util.entity.RegionalFood;
import com.piggy.piggybackend.shopping.util.repository.RegionalFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
}