package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.global.exception.CustomException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // Expo 개발 서버 주소 (환경변수 APP_EXPO_HOST, 기본값 localhost:8081)
    @Value("${app.expo-host:localhost:8081}")
    private String expoHost;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 1. 에러 메시지 추출 및 인코딩
        String rawMessage = "로그인 중 오류가 발생했습니다.";
        if (exception.getCause() instanceof CustomException) {
            rawMessage = exception.getCause().getMessage();
        } else if (exception.getMessage() != null) {
            rawMessage = exception.getMessage();
        }
        String errorMessage = URLEncoder.encode(rawMessage, StandardCharsets.UTF_8);

        // 2. SuccessHandler와 동일한 앱/웹 판별 로직
        String target = request.getParameter("target");
        if (target == null) {
            target = (String) request.getSession().getAttribute("target");
        }
        String userAgent = request.getHeader("User-Agent");

        // 앱 여부 판단 (모바일 기기거나 target 파라미터가 'app'인 경우)
        boolean isApp = "app".equals(target) || (target == null && userAgent != null && userAgent.toLowerCase().contains("mobi"));

        String targetUrl;
        if (isApp) {
            // 엑스포 앱용 에러 경로
            targetUrl = String.format("exp://%s/--/oauth/error?message=%s", expoHost, errorMessage);
        } else {
            // 웹용 에러 경로
            targetUrl = String.format("http://localhost:5173/oauth/error?message=%s", errorMessage);
        }

        log.info("Redirecting to (error): {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}