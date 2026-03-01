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
        log.info("[컨트롤러] OCR 스캔 요청 도착!");
        // 이미지를 처리하고 결과를 반환
        OcrResponse response = ocrService.processImage(request.getImage());
        return ApiResponse.success(response);
    }
}