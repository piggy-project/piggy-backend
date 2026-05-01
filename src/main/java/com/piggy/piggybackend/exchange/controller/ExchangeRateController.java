package com.piggy.piggybackend.exchange.controller;

import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeHistoryResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeRankingResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeRateResponseDto;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    // 상세 환율 조회
    @GetMapping("/detail/{code}")
    public ResponseEntity<ExchangeDetailResponseDto> getExchangeDetail(
            @PathVariable String code) {
        return ResponseEntity.ok(exchangeRateService.getExchangeDetail(code));
    }

    // 환율 히스토리 조회
    @GetMapping("/history")
    public ResponseEntity<List<ExchangeHistoryResponseDto>> getExchangeHistory(
            @RequestParam String code) { // @PathVariable 대신 @RequestParam 사용
        return ResponseEntity.ok(exchangeRateService.getExchangeHistory(code));
    }
    // 일일 시간별 환율 히스토리 조회 (09:00 ~ 22:00)
    @GetMapping("/history/daily/{code}")
    public ResponseEntity<List<ExchangeHistoryResponseDto>> getDailyHourlyHistory(
            @PathVariable String code) {
        return ResponseEntity.ok(exchangeRateService.getDailyHourlyHistory(code));
    }

    // 환율 리스트 및 랭킹 조회
    // sort: RISE(급상승), FALL(급하락), HIGH(환율 높은순), LOW(환율 낮은순)
    // 예시: GET /api/exchange/ranking?sort=RISE
    @GetMapping("/ranking")
    public ResponseEntity<List<ExchangeRankingResponseDto>> getExchangeRanking(
            @RequestParam(defaultValue = "RISE") String sort) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRanking(sort));
    }
}