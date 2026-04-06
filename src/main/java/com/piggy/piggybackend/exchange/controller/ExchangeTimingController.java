package com.piggy.piggybackend.exchange.controller;

import com.piggy.piggybackend.exchange.dto.ExchangeTimingResponseDto;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class ExchangeTimingController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/timing")
    public ResponseEntity<ExchangeTimingResponseDto> getTiming(@RequestParam String code) {
        return ResponseEntity.ok(exchangeRateService.getExchangeTiming(code));
    }
}
