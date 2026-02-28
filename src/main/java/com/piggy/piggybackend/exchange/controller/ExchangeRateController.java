package com.piggy.piggybackend.exchange.controller;

import com.piggy.piggybackend.exchange.dto.ExchangeDetailResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeHistoryResponseDto;
import com.piggy.piggybackend.exchange.dto.ExchangeRateResponseDto;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    //환율 목록 조회
    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateResponseDto>> getRates() {
        return ResponseEntity.ok(exchangeRateService.getLatestRates());
    }
    //환율 상세 조회(환율, 증감율, 최고, 최저 등등)
    @GetMapping("/detail/{code}")
    public ResponseEntity<ExchangeDetailResponseDto> getDetail(@PathVariable String code) {
        // 서비스에서 만든 getExchangeDetail 메서드를 호출합니다.
        return ResponseEntity.ok(exchangeRateService.getExchangeDetail(code));
    }
    //환율 과거 조회(기간 별 환율 조회)
    @GetMapping("/history")
    public ResponseEntity<List<ExchangeHistoryResponseDto>> getHistory(@RequestParam String code) {
        return ResponseEntity.ok(exchangeRateService.getExchangeHistory(code));
    }
}
