package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.global.util.JwtUtil;
import com.piggy.piggybackend.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getEmailFromAttributes(attributes); // 이메일 파싱 로직 분리(깔끔하게)

        if (email == null) {
            email = oAuth2User.getName();
        }
        boolean isNewUser = userRepository.findByEmail(email)
                .map(user -> user.getBirth().equals(java.time.LocalDate.of(1900, 1, 1))) // 유빈님이 설정한 더미 날짜 체크
                .orElse(false);

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        String target = request.getParameter("target");
        if (target == null) {
            target = (String) request.getSession().getAttribute("target");
        }

        String userAgent = request.getHeader("User-Agent");
        String targetUrl;

        // 앱웹 여부 판단
        boolean isApp = "app".equals(target) || (target == null && userAgent != null && userAgent.toLowerCase().contains("mobi"));

        // 경로 결정 (추가 정보 입력 여부에 따른 분기)
        String path = isNewUser ? "additional-info" : "success";

        if (isApp) {
            // 엑스포 앱용 (경로에 path 변수 삽입)
            targetUrl = String.format("exp://localhost:8081/--/oauth/%s?token=%s&refreshToken=%s",
                    path, accessToken, refreshToken);
        } else {
            // 웹용
            targetUrl = String.format("https://piggys.mooo.com/oauth/%s?token=%s&refreshToken=%s",
                    path, accessToken, refreshToken);
        }

        System.out.println("Redirecting to (" + path + "): " + targetUrl);
        response.sendRedirect(targetUrl);
    }

    // 이메일 추출 로직 (가독성을 위해 메서드 추출)
    private String getEmailFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) {
            return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
        } else if (attributes.containsKey("response")) {
            return (String) ((Map<String, Object>) attributes.get("response")).get("email");
        }
        return (String) attributes.get("email");
    }
}