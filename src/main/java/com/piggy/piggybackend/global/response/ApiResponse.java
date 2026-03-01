package com.piggy.piggybackend.global.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int statusCode;
    private T data;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .statusCode(200)
                .data(data)
                .message(null)
                .build();
    }

    public static <T> ApiResponse<T> created(String message) {
        return ApiResponse.<T>builder()
                .statusCode(201)
                .data(null)
                .message(message)
                .build();
    }
}
