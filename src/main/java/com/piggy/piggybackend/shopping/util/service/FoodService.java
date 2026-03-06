package com.piggy.piggybackend.shopping.util.service;

import com.piggy.piggybackend.shopping.util.dto.FoodResponse;
import com.piggy.piggybackend.shopping.util.repository.RegionalFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodService {
    private final RegionalFoodRepository foodRepository;

    @Transactional(readOnly = true)
    public List<FoodResponse> getFoodsByCountry(String countryCode) {
        return foodRepository.findByCountryCodeOrderByRankAsc(countryCode)
                .stream()
                .map(FoodResponse::new)
                .collect(Collectors.toList());
    }
}
