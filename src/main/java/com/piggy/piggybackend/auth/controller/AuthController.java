package com.piggy.piggybackend.auth.controller;

import com.piggy.piggybackend.auth.dto.RefreshRequest;
import com.piggy.piggybackend.auth.dto.TokenResponse;
import com.piggy.piggybackend.auth.service.AuthService;
import com.piggy.piggybackend.global.response.ApiResponse;
import com.piggy.piggybackend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return authService.reissue(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@AuthenticationPrincipal String email) {
        authService.logout(email);
        return ApiResponse.success("로그아웃 완료");
    }
}