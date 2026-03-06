package com.piggy.piggybackend.shopping.util.controller;

import com.piggy.piggybackend.shopping.util.dto.FoodResponse;
import com.piggy.piggybackend.shopping.util.service.FoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseEntity<List<FoodResponse>> getRegionalFoods(@RequestParam String currencyCode) {
        // 서비스에서 랜덤으로 5개를 뽑아옵니다.
        List<FoodResponse> randomFoods = foodService.getRandomFiveFoods(currencyCode);
        return ResponseEntity.ok(randomFoods);
    }
}