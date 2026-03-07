package com.piggy.piggybackend.exchange.service;

import com.piggy.piggybackend.exchange.constant.BankPolicy;
import com.piggy.piggybackend.exchange.dto.ExchangeSimulateRequest;
import com.piggy.piggybackend.exchange.dto.ExchangeSimulateResponse;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeSimulatorService {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_LIVE_RATES = "exchange:live";

    public ExchangeSimulateResponse simulate(ExchangeSimulateRequest request) {
        // 1. Redis에서 실시간 매매기준율 조회
        Object rateObj = redisTemplate.opsForHash().get(REDIS_LIVE_RATES, request.getTargetCurrency());
        if (rateObj == null) {
            throw new CustomException(ErrorCode.EXCHANGE_DATA_NOT_FOUND, "현재 환율 정보를 불러올 수 없습니다.");
        }
        double baseRate = Double.parseDouble(rateObj.toString());

        // 2. 수수료율 & 우대율 세팅
        double feeRatePct = 0.0;
        double discountRatePct = 0.0;

        if (request.getBankPolicy() == BankPolicy.CUSTOM) {
            feeRatePct = request.getCustomFeeRate() != null ? request.getCustomFeeRate() : 0.0;
            discountRatePct = request.getCustomDiscountRate() != null ? request.getCustomDiscountRate() : 0.0;
        } else {
            feeRatePct = request.getBankPolicy().getDefaultFeeRate();
            discountRatePct = request.getBankPolicy().getMaxDiscountRate();
        }

        // 백분율(%)을 소수로 변환 (예: 1.75% -> 0.0175)
        double feeRate = feeRatePct / 100.0;
        double discountRate = discountRatePct / 100.0;

        // 3. 환율 계산 (수학 공식)
        // 외화 1단위당 기본 수수료 = 매매기준율 * 수수료율
        double baseFeePerUnit = baseRate * feeRate;

        // 외화 1단위당 실제 부과되는 수수료 = 기본 수수료 * (1 - 우대율)
        double actualFeePerUnit = baseFeePerUnit * (1 - discountRate);

        // 최종 적용 환율 = 매매기준율 + 실제 부과 수수료
        double appliedRate = baseRate + actualFeePerUnit;

        // 최종 수령할 외화 = 입력한 원화 / 최종 적용 환율
        double finalForeignAmount = request.getKrwAmount() / appliedRate;

        // 4. 이득 계산 (얼마나 아꼈는가?)
        // 아낀 수수료 1단위 = 기본 수수료 - 실제 부과 수수료
        double savedFeePerUnit = baseFeePerUnit - actualFeePerUnit;

        // 총 아낀 금액(원) = 아낀 수수료 1단위 * 최종 수령 외화
        long savedFeeKrw = Math.round(savedFeePerUnit * finalForeignAmount);
        long originalFeeKrw = Math.round(baseFeePerUnit * finalForeignAmount);

        // 5. 응답 조립 (소수점 둘째 자리까지 반올림)
        return ExchangeSimulateResponse.builder()
                .currency(request.getTargetCurrency())
                .baseRate(baseRate)
                .appliedRate(Math.round(appliedRate * 100) / 100.0)
                .inputKrwAmount(request.getKrwAmount())
                .finalForeignAmount(Math.round(finalForeignAmount * 100) / 100.0)
                .originalFeeKrw(originalFeeKrw)
                .savedFeeKrw(savedFeeKrw) // 🔥 프론트에서 "토스뱅크에서 OO원을 아꼈어요!" 띄울 값
                .build();
    }
}