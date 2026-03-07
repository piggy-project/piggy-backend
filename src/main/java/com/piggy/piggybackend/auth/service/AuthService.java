package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.auth.dto.TokenResponse;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtil jwtUtil;

    // RedisTemplate 주입 (String, String 형태)
    private final RedisTemplate<String, String> redisTemplate;

    public TokenResponse reissue(String refreshToken) {

        // 1️⃣ 토큰 유효성
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰");
        }

        // 2️⃣ 타입 체크
        String type = jwtUtil.extractType(refreshToken);
        if (!"refresh".equals(type)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 아닙니다");
        }

        // 3️⃣ 이메일 추출 (이 이메일이 Redis의 Key가 됨)
        String email = jwtUtil.extractEmail(refreshToken);

        // 4️⃣ Redis에서 저장된 리프레시 토큰 가져오기 (User DB 조회 쿼리 제거됨!)
        String savedToken = redisTemplate.opsForValue().get("RT:" + email);
        if (savedToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "로그아웃 되거나 만료된 리프레시 토큰입니다.");
        }

        if (!savedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰 불일치");
        }

        // 5️⃣ Rotation (새 토큰 발급)
        String newAccess = jwtUtil.generateAccessToken(email);
        String newRefresh = jwtUtil.generateRefreshToken(email);

        // 6️⃣ Redis에 새 토큰 덮어쓰기 (TTL 14일 재설정)
        redisTemplate.opsForValue().set("RT:" + email, newRefresh, 14, TimeUnit.DAYS);

        return new TokenResponse(newAccess, newRefresh);
    }

    public void logout(String email) {
        // Redis에서 해당 이메일의 리프레시 토큰 삭제
        // (마찬가지로 User DB 조회 로직 뺄 수 있음)
        redisTemplate.delete("RT:" + email);
    }
}