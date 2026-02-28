package com.piggy.piggybackend.exchange.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//외부에서 받아오는 환율 데이터 포멧
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeximRateResponseDto {
    private Integer result;      // 결과 (1: 성공)
    @JsonProperty("cur_unit")
    private String curUnit;      // 통화코드
    @JsonProperty("cur_nm")
    private String curNm;        // 통화명
    @JsonProperty("deal_bas_r")
    private String dealBasR;     // 매매기준율 (문자열로 옴: "1,350.50")

    // 숫자로 변환하는 헬퍼 메서드
    public Double getNumericRate() {
        if (dealBasR == null) return 0.0;
        return Double.parseDouble(dealBasR.replace(",", ""));
    }
}