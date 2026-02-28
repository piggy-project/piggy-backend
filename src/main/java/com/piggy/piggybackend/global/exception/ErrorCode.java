package com.piggy.piggybackend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "DuplicateEmail", "이미 등록된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "DuplicateNickname", "이미 사용중인 닉네임입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "ValidationError", "비밀번호가 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "InternalServerError", "서버에 문제가 발생했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "InvalidCredentials", "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    // 환율 관련 에러 코드
    GRAPH_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "ExchangeGraphNotSupported", "환율 그래프를 지원하지 않습니다."),
    EXCHANGE_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "ExchangeDataNotFound", "환율 데이터를 찾을 수 없습니다."),
    API_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ApiServerError", "외부 환율 API 호출 중 에러가 발생했습니다."),
    //리프레시 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "InvalidToken", "유효하지 않은 토큰"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "ExpiredToken", "만료된 토큰입니다"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "SamePassword", "기존 비밀번호와 동일합니다.");
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
