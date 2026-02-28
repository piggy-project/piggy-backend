package com.piggy.piggybackend.global.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private int statusCode;
    private String errorCode;
    private String message;
}
