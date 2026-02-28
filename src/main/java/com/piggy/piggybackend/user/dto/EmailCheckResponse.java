package com.piggy.piggybackend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class    EmailCheckResponse {
    @JsonProperty("isAvailable") //변수 이름 고정
    private boolean isAvailable;
}
