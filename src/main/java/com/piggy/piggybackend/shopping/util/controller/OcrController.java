package com.piggy.piggybackend.shopping.util.controller;

import com.piggy.piggybackend.global.response.ApiResponse;
import com.piggy.piggybackend.shopping.util.dto.OcrRequest;
import com.piggy.piggybackend.shopping.util.dto.OcrResponse;
import com.piggy.piggybackend.shopping.util.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ocr")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/scan")
    public ApiResponse<OcrResponse> scanPrice(@RequestBody OcrRequest request) {
        log.info("OCR 스캔 요청 - 통화: {}", request.getCurrency());
        // 이미지와 통화 코드를 함께 전달
        OcrResponse response = ocrService.processImage(request.getImage(), request.getCurrency());
        return ApiResponse.success(response);
    }
}