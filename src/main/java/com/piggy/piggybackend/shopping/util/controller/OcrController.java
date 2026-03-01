package com.piggy.piggybackend.shopping.util.controller;

import com.piggy.piggybackend.global.response.ApiResponse;
import com.piggy.piggybackend.shopping.util.dto.OcrRequest;
import com.piggy.piggybackend.shopping.util.dto.OcrResponse;
import com.piggy.piggybackend.shopping.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ocr")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/scan")
    public ApiResponse<OcrResponse> scanPrice(@RequestBody OcrRequest request) {
        // 이미지를 처리하고 결과를 반환
        OcrResponse response = ocrService.processImage(request.getImage());
        return ApiResponse.success(response);
    }
}