package com.piggy.piggybackend.global.util;

import com.piggy.piggybackend.user.entity.User;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @LoginUser 어노테이션이 붙어 있고, 타입이 User(엔티티)인 경우에만 작동
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 📍 여기서 세션이나 시큐리티 컨텍스트에서 유저 객체를 꺼내서 반환합니다.
        // 예: return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return null; // 본인의 인증 로직에 맞춰 구현!
    }
}