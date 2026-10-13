package com.piggy.piggybackend.global.exception;

import com.piggy.piggybackend.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .statusCode(400)
                        .errorCode("InvalidParameter")
                        .message("잘못된 요청 값입니다: " + e.getName())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("Unhandled exception", e);

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.builder()
                        .statusCode(500)
                        .errorCode("InternalServerError")
                        .message("서버 오류가 발생했습니다.")
                        .build());
    }
}
