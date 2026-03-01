package com.piggy.piggybackend.shopping.util.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.piggy.piggybackend.exchange.service.ExchangeRateService;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.shopping.util.dto.OcrResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final ExchangeRateService exchangeRateService; // 기존 환율 서비스 활용

    public OcrResponse processImage(String base64Image) {
        // 1. Google Vision API로 텍스트 인식
        String detectedText = detectTextFromImage(base64Image);
        log.info("인식된 텍스트: {}", detectedText);

        // 2. 텍스트에서 금액 숫자만 추출
        double jpyAmount = parsePrice(detectedText);

        // 3. 실시간 JPY 환율 정보 가져오기
        var jpyDetail = exchangeRateService.getExchangeDetail("JPY");
        if (jpyDetail == null) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);
        }
        double currentRate = jpyDetail.getCurrentRate();

        // 4. 원화 계산 (엔화는 100단위 기준이므로 100으로 나눕니다)
        long krwAmount = Math.round((jpyAmount * currentRate) / 100);

        return OcrResponse.builder()
                .originalText(detectedText.length() > 20 ? detectedText.substring(0, 20) + "..." : detectedText)
                .jpyAmount(jpyAmount)
                .krwAmount(krwAmount)
                .build();
    }

    private String detectTextFromImage(String base64Image) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Image);
            ByteString imgBytes = ByteString.copyFrom(data);

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
                AnnotateImageResponse res = response.getResponses(0);

                if (res.hasError()) throw new Exception(res.getError().getMessage());

                return res.getFullTextAnnotation().getText();
            }
        } catch (Exception e) {
            log.error("OCR 스캔 에러: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private double parsePrice(String text) {
        // 숫자와 쉼표를 찾아내는 정규식 (가격표의 숫자를 타겟팅)
        Pattern pattern = Pattern.compile("([0-9]{1,3}(,[0-9]{3})*|[0-9]+)");
        Matcher matcher = pattern.matcher(text);

        double maxFound = 0;
        while (matcher.find()) {
            try {
                String val = matcher.group().replace(",", "");
                double num = Double.parseDouble(val);
                if (num > maxFound) maxFound = num;
            } catch (NumberFormatException ignored) {}
        }

        if (maxFound <= 0) throw new CustomException(ErrorCode.EMPTY_SEARCH_RESULT);
        return maxFound;
    }
}