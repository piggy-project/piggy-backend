package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.auth.dto.TokenResponse;
import com.piggy.piggybackend.auth.entity.RefreshToken;
import com.piggy.piggybackend.auth.repository.RefreshTokenRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.util.JwtUtil;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenResponse reissue(String refreshToken) {

        // 1️⃣ 토큰 유효성
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰");
        }

        // 2️⃣ 타입 체크 (refresh인지)
        String type = jwtUtil.extractType(refreshToken);
        if (!"refresh".equals(type)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 아닙니다");
        }

        // 3️⃣ 사용자 조회
        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4️⃣ DB에 저장된 refresh와 비교
        RefreshToken saved = refreshTokenRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN, "저장된 리프레시 토큰 없음"));

        if (!saved.getToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰 불일치");
        }

        // 5️⃣ Rotation (새 토큰 발급)
        String newAccess = jwtUtil.generateAccessToken(email);
        String newRefresh = jwtUtil.generateRefreshToken(email);

        saved.rotate(newRefresh, LocalDateTime.now().plusDays(14));

        return new TokenResponse(newAccess, newRefresh);
    }

    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteById(user.getId());
    }
}