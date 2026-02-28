package com.piggy.piggybackend.user.controller;

import com.piggy.piggybackend.global.response.ApiResponse;
import com.piggy.piggybackend.user.dto.*;
import com.piggy.piggybackend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    //회원가입
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> signUp(
            @Valid @RequestBody UserCreateRequest request) {

        userService.signUp(request);

        return ApiResponse.success("가입이 완료되었습니다.");
    }
    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);

        return ResponseEntity.status(201).body(response);
    }

    //이메일 중복체크
    @PostMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @RequestParam String email
    ) {
        EmailCheckResponse response = userService.checkEmail(email);
        return ResponseEntity.ok(response);
    }
    //닉네임 중복체크
    @PostMapping("/check-nickname")
    public ResponseEntity<EmailCheckResponse> checkNickname(
            @RequestParam String nickname
    ) {
        EmailCheckResponse response = userService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }
    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(
            @AuthenticationPrincipal String email
    ) {
        // 1. 토큰에서 추출한 email로 유저 조회
        UserInfoResponse userInfo = userService.getMyInfo(email);

        return ResponseEntity.ok(userInfo);
    }

    //비밀번호 재설정
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> requestReset(@RequestParam String email) {
        userService.createResetToken(email);
        return ResponseEntity.ok("이메일 발송 완료");
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @RequestBody PasswordResetDto dto
    ) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok("비밀번호 변경 완료");
    }
}
