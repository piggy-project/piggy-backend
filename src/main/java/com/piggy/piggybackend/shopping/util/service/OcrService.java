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
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final ExchangeRateService exchangeRateService; // 기존 환율 서비스 활용

    public OcrResponse processImage(String base64Image, String currencyCode) {
        String detectedText = detectTextFromImage(base64Image);
        log.info("[{}] 인식된 전체 텍스트: {}", currencyCode, detectedText);

        // 1. 통화별 맞춤형 금액 추출
        double foreignAmount = parsePriceByCurrency(detectedText, currencyCode);
        String originalText = formatOriginalText(foreignAmount, currencyCode);

        // 2. 해당 통화의 실시간 환율 정보 조회
        var exchangeDetail = exchangeRateService.getExchangeDetail(currencyCode);
        if (exchangeDetail == null) throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND);

        double currentRate = exchangeDetail.getCurrentRate();

        // 3. 원화 계산 (JPY, VND 등 100단위 통화 처리)
        // 한국 환율 정보상 JPY, VND, IDR 등은 보통 100단위당 원화로 표기됨을 고려
        long krwAmount;
        if (isHundredUnitCurrency(currencyCode)) {
            krwAmount = Math.round((foreignAmount * currentRate) / 100);
        } else {
            krwAmount = Math.round(foreignAmount * currentRate);
        }

        return OcrResponse.builder()
                .originalText(originalText)
                .currencyCode(currencyCode)
                .foreignAmount(foreignAmount)
                .krwAmount(krwAmount)
                .exchangeRate(currentRate)
                .build();
    }

    private double parsePriceByCurrency(String text, String currencyCode) {
        // 해당 통화에 특화된 심볼 패턴 가져오기
        String symbolPattern = getSymbolPattern(currencyCode);

        // 1. 기호와 숫자가 함께 있는 패턴 검색 (예: $10.99, 500円, 150.00€)
        // 소수점(.)이 포함될 수 있는 숫자를 찾도록 개선
        Pattern pattern = Pattern.compile("([0-9,]+(\\.[0-9]{1,2})?)\\s*(" + symbolPattern + ")|(" + symbolPattern + ")\\s*([0-9,]+(\\.[0-9]{1,2})?)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            try {
                // 그룹 중 숫자가 있는 부분을 찾아 추출 (기호가 앞일 때와 뒤일 때 대응)
                String val = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(5);
                return Double.parseDouble(val.replace(",", ""));
            } catch (NumberFormatException ignored) {}
        }

        // 2. 기호 매칭 실패 시 텍스트 내 가장 큰 숫자 추출 (fallback)
        return parseMaxNumber(text);
    }

    private double parseMaxNumber(String text) {
        Pattern plainNumberPattern = Pattern.compile("([0-9,]+(\\.[0-9]{1,2})?)");
        Matcher numberMatcher = plainNumberPattern.matcher(text);

        double maxFound = 0;
        while (numberMatcher.find()) {
            try {
                double num = Double.parseDouble(numberMatcher.group().replace(",", ""));
                if (num > maxFound) maxFound = num;
            } catch (NumberFormatException ignored) {}
        }

        if (maxFound <= 0) throw new CustomException(ErrorCode.EMPTY_SEARCH_RESULT);
        return maxFound;
    }

    private String getSymbolPattern(String currencyCode) {
        return switch (currencyCode) {
            case "USD" -> "\\$|USD|US";
            case "EUR" -> "€|EUR";
            case "JPY" -> "円|¥|YEN";
            case "THB" -> "฿|Baht|B";
            case "TWD" -> "NT\\$|元|TWD";
            case "PHP" -> "₱|Peso|PHP";
            default -> "\\$|¥|€|円";
        };
    }

    private String formatOriginalText(double amount, String currencyCode) {
        return switch (currencyCode) {
            case "USD" -> "$" + amount;
            case "EUR" -> amount + "€";
            case "JPY" -> amount + "円";
            case "THB" -> amount + "฿";
            default -> amount + " " + currencyCode;
        };
    }

    private boolean isHundredUnitCurrency(String currencyCode) {
        // 한국 매매기준율상 100단위로 고시되는 통화들
        return List.of("JPY", "VND", "IDR").contains(currencyCode);
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
}