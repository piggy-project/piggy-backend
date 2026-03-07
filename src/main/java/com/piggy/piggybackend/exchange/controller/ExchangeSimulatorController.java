package com.piggy.piggybackend.exchange.controller;

import com.piggy.piggybackend.exchange.dto.ExchangeSimulateRequest;
import com.piggy.piggybackend.exchange.dto.ExchangeSimulateResponse;
import com.piggy.piggybackend.exchange.service.ExchangeSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange/simulate")
public class ExchangeSimulatorController {

    private final ExchangeSimulatorService simulatorService;

    @PostMapping
    public ResponseEntity<ExchangeSimulateResponse> simulateExchange(
            @RequestBody ExchangeSimulateRequest request) {
        return ResponseEntity.ok(simulatorService.simulate(request));
    }
}