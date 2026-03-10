package com.piggy.piggybackend.auth.service;

import com.piggy.piggybackend.auth.dto.*;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.user.entity.Provider;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Value("${app.default-image-url}")
    private String defaultProfileImageUrl;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);

        // 1. yml에 설정한 id, response, sub 같은 키값을 가져옴
        String userNameAttributeName = request.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        String registrationId = request.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        User user = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .orElseGet(() -> {
                    userRepository.findByEmail(userInfo.getEmail())
                            .ifPresent(u -> {
                                String korProvider = getProviderKorName(u.getProvider());
                                String message = String.format("이미 %s로 가입된 계정입니다. %s로 로그인해주세요.", korProvider, korProvider);

                                throw new OAuth2AuthenticationException(new OAuth2Error("duplicate_email"), message);
                            });
                    return registerUser(provider, userInfo);
                });

        // 2. userNameAttributeName을 동적으로 할당
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }

    private User registerUser(Provider provider, OAuth2UserInfo userInfo) {
        User savedUser = userRepository.save(User.builder()
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .email(userInfo.getEmail())
                .nickname(generateRandomNickname())

                // 1. password: 소셜 유저는 비번이 없으므로 랜덤값 주입
                .password(UUID.randomUUID().toString())

                // 2. name: 구글에서 이름을 못 가져올 경우를 대비해 기본값 설정
                .name(userInfo.getName() != null ? userInfo.getName() : "소셜사용자")

                // 3. birth: DB가 NULL을 허용하지 않으므로 임시 날짜 주입
                .birth(LocalDate.of(1900, 1, 1))

                // 4. phoneNum: 이번 에러 해결! 임시 번호 주입
                .phoneNum("010-0000-0000")

                .profileImageUrl(defaultProfileImageUrl)

                .build());
        return savedUser;
    }

    private OAuth2UserInfo getOAuth2UserInfo(
            String registrationId,
            java.util.Map<String, Object> attributes
    ) {

        switch (registrationId) {
            case "kakao":
                return new KakaoUserInfo(attributes);
            case "naver":
                return new NaverUserInfo(attributes);
            case "google":
                return new GoogleUserInfo(attributes);
            default:
                throw new IllegalArgumentException("Unsupported provider");
        }
    }

    private String generateRandomNickname() {
        return "U" + UUID.randomUUID().toString().substring(0, 6);
    }
    private String getProviderKorName(Provider provider) {
        if (provider == null) return "알 수 없는 계정";
        return switch (provider) {
            case KAKAO -> "카카오";
            case NAVER -> "네이버";
            case GOOGLE -> "구글";
            case LOCAL -> "일반 이메일";
            default -> provider.name();
        };
    }
}
