package com.piggy.piggybackend.global.exception;

import com.piggy.piggybackend.global.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .statusCode(errorCode.getHttpStatus().value())
                        .errorCode(errorCode.getErrorCode())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .statusCode(400)
                        .errorCode("ValidationError")
                        .message(message)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {   // 개발 디버깅용 나중에 다시 500 으로

        e.printStackTrace();   // 👈 이 줄 추가

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.builder()
                        .statusCode(500)
                        .errorCode("InternalServerError")
                        .message(e.getMessage())   // 👈 실제 메시지 보기
                        .build());
    }
}
