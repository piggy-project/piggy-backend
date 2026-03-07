package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.global.util.JwtUtil;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate; // ✨ 추가됨

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getEmailFromAttributes(attributes);

        if (email == null) {
            email = oAuth2User.getName();
        }

        boolean isNewUser = userRepository.findByEmail(email)
                .map(user -> user.getBirth().equals(java.time.LocalDate.of(1900, 1, 1)))
                .orElse(false);

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // ✨ Redis에 리프레시 토큰 저장 (TTL 14일)
        redisTemplate.opsForValue().set("RT:" + email, refreshToken, 14, TimeUnit.DAYS);

        // ... (이하 기존 redirect 로직과 동일) ...
        String target = request.getParameter("target");
        if (target == null) {
            target = (String) request.getSession().getAttribute("target");
        }

        String userAgent = request.getHeader("User-Agent");
        boolean isApp = "app".equals(target) || (target == null && userAgent != null && userAgent.toLowerCase().contains("mobi"));
        String path = isNewUser ? "additional-info" : "success";

        String targetUrl;
        if (isApp) {
            targetUrl = String.format("exp://localhost:8081/--/oauth/%s?token=%s&refreshToken=%s",
                    path, accessToken, refreshToken);
        } else {
            targetUrl = String.format("http://localhost:5173/oauth/%s?token=%s&refreshToken=%s",
                    path, accessToken, refreshToken);
        }

        System.out.println("Redirecting to (" + path + "): " + targetUrl);
        response.sendRedirect(targetUrl);
    }

    private String getEmailFromAttributes(Map<String, Object> attributes) {
        // 기존 코드 동일
        if (attributes.containsKey("kakao_account")) {
            return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
        } else if (attributes.containsKey("response")) {
            return (String) ((Map<String, Object>) attributes.get("response")).get("email");
        }
        return (String) attributes.get("email");
    }
}