package com.piggy.piggybackend.global.util;

import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @LoginUser 어노테이션이 붙어 있고, 타입이 User(엔티티)인 경우에만 작동합니다.
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // 1. SecurityContextHolder에서 현재 인증된 정보를 꺼냅니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 인증 정보가 없거나 익명 사용자라면 null을 반환합니다.
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        // 3. 인증 정보(보통 이메일)를 사용하여 DB에서 실제 유저 객체를 찾습니다.
        // JwtAuthenticationFilter에서 setAuthentication에 넣은 값이 여기서의 getName()이 됩니다.
        String email = authentication.getName();

        // 4. DB에서 조회한 '영속 상태'의 유저를 반환해야 에러가 나지 않습니다.
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}