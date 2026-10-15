package com.piggy.piggybackend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "DUPLICATE_EMAIL", "이미 등록된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "DUPLICATE_NICKNAME", "이미 사용중인 닉네임입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버에 문제가 발생했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    // 환율 관련 에러 코드
    GRAPH_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "GRAPH_NOT_SUPPORTED", "환율 그래프를 지원하지 않습니다."),
    EXCHANGE_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "EXCHANGE_DATA_NOT_FOUND", "환율 데이터를 찾을 수 없습니다."),
    API_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API_SERVER_ERROR", "외부 환율 API 호출 중 에러가 발생했습니다."),
    //리프레시 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "기존 비밀번호와 동일합니다."),

    SHOPPING_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "SHOPPING_API_ERROR", "쇼핑몰 검색 API 호출 중 문제가 발생했습니다."),
    CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CACHE_ERROR", "데이터 캐싱 처리 중 문제가 발생했습니다."),
    EMPTY_SEARCH_RESULT(HttpStatus.NOT_FOUND, "EMPTY_SEARCH_RESULT", "검색 결과가 존재하지 않습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // 커뮤니티 관련 에러 코드 추가
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "존재하지 않는 게시글입니다."),
    NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED", "해당 작업을 수행할 권한이 없습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND","댓글을 찾을 수 없습니다."),

    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "USER_WITHDRAWN", "탈퇴 진행 중인 회원입니다."),
    // 알림 관련 에러 코드 추가
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "존재하지 않는 알림 설정입니다.");
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
