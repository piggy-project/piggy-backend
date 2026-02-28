package com.piggy.piggybackend.exchange.dto;

import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
//프론트 전달용 DTO
public class ExchangeRateResponseDto {
    private String curUnit;
    private String curNm;
    private Double rate;
    private String date;
    private String time;

    // DB 엔티티로부터 변환할 때 (주로 과거 기록 조회용)
    public static ExchangeRateResponseDto from(ExchangeRate entity) {
        String unit = entity.getCurUnit();
        if (unit != null && unit.startsWith("JPY")) {
            unit = "JPY";
        }

        return ExchangeRateResponseDto.builder()
                .curUnit(unit)
                .curNm(entity.getCurNm())
                .rate(entity.getDealBasR())
                .date(entity.getSearchDate())
                .time("") // 엔티티에는 시간이 없으므로 빈 값 처리
                .build();
    }
}