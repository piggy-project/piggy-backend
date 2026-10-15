package com.piggy.piggybackend.global.config;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.redis.service.ApiQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

// 외부 유료 API(SerpAPI 등)를 호출하는 엔드포인트에 IP 단위 분당 요청 제한
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ApiQuotaService apiQuotaService;

    @Value("${rate-limit.shopping-per-minute:10}")
    private int limitPerMinute;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // forward-headers-strategy: framework 설정으로 프록시 뒤에서도 클라이언트 IP가 들어옴
        String key = request.getRequestURI() + ":" + request.getRemoteAddr();

        if (!apiQuotaService.tryAcquire(key, limitPerMinute, Duration.ofMinutes(1))) {
            response.setHeader("Retry-After", "60");
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return true;
    }
}
