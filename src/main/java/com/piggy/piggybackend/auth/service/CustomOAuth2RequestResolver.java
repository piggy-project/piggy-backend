package com.piggy.piggybackend.auth.service;

import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class CustomOAuth2RequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2RequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        // 기본 제공하는 리졸버를 내부에 장착합니다. Base 경로인 /oauth2/authorization를 사용합니다.
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
        return saveRedirectUrlToSession(request, authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
        return saveRedirectUrlToSession(request, authRequest);
    }

    private OAuth2AuthorizationRequest saveRedirectUrlToSession(
            HttpServletRequest request, OAuth2AuthorizationRequest authRequest) {

        if (authRequest == null) {
            return null;
        }

        // 최초 요청(?currentRedirectUrl=...)이 들어왔을 때 파라미터를 세션에 구워버립니다.
        String currentRedirectUrl = request.getParameter("currentRedirectUrl");
        if (currentRedirectUrl != null) {
            HttpSession session = request.getSession();
            session.setAttribute("currentRedirectUrl", currentRedirectUrl);
            System.out.println("[OAuth2 Resolver] 세션에 currentRedirectUrl 저장 완료: " + currentRedirectUrl);
        }

        return authRequest;
    }
}