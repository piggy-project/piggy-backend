package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = null;

        // [기존 로직] 제공자별 이메일 파싱
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            email = (String) kakaoAccount.get("email");
        } else if (attributes.containsKey("response")) {
            Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
            email = (String) naverResponse.get("email");
        } else {
            email = (String) attributes.get("email");
        }

        if (email == null) {
            email = oAuth2User.getName();
        }

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // 1. 세션이나 파라미터에서 target 확인 (유실될 가능성 높음)
        String target = request.getParameter("target");
        if (target == null) {
            target = (String) request.getSession().getAttribute("target");
        }

        // 2. User-Agent 확인 (기기 정보)
        String userAgent = request.getHeader("User-Agent");

        String targetUrl;

        // 💡 [수정된 분기 로직]
        // target이 직접 "app"으로 왔거나,
        // 정보가 없는데 기기가 모바일(Mobi 포함)이라면 앱으로 리다이렉트
        if ("app".equals(target) || (target == null && userAgent != null && userAgent.toLowerCase().contains("mobi"))) {
            // 📱 엑스포(앱)용 리다이렉트
            targetUrl = "exp://localhost:8081/--/oauth/success?token=" + accessToken + "&refreshToken=" + refreshToken;
            System.out.println("Redirecting to App: " + targetUrl);
        } else {
            // 🌐 웹용 리다이렉트 (PC 브라우저 등)
            targetUrl = "https://piggys.mooo.com/oauth/success?token=" + accessToken + "&refreshToken=" + refreshToken;
            System.out.println("Redirecting to Web: " + targetUrl);
        }

        response.sendRedirect(targetUrl);
    }
}