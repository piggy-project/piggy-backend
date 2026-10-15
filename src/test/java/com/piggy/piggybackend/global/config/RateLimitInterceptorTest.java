package com.piggy.piggybackend.global.config;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.redis.service.ApiQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    private final Map<String, Long> counters = new HashMap<>();
    private StringRedisTemplate redisTemplate;
    private ApiQuotaService apiQuotaService;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Redis INCR을 메모리 Map으로 흉내
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenAnswer(inv -> counters.merge(inv.getArgument(0), 1L, Long::sum));

        apiQuotaService = new ApiQuotaService(redisTemplate);
        interceptor = new RateLimitInterceptor(apiQuotaService);
        ReflectionTestUtils.setField(interceptor, "limitPerMinute", 3);
    }

    private MockHttpServletRequest request(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/shopping/compare");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void 분당_한도까지는_통과하고_초과하면_429() {
        for (int i = 0; i < 3; i++) {
            assertThat(interceptor.preHandle(request("1.1.1.1"), new MockHttpServletResponse(), null)).isTrue();
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> interceptor.preHandle(request("1.1.1.1"), response, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void IP별로_따로_카운트한다() {
        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(request("1.1.1.1"), new MockHttpServletResponse(), null);
        }
        assertThat(interceptor.preHandle(request("2.2.2.2"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void 첫_요청에만_TTL을_건다() {
        interceptor.preHandle(request("1.1.1.1"), new MockHttpServletResponse(), null);
        interceptor.preHandle(request("1.1.1.1"), new MockHttpServletResponse(), null);

        verify(redisTemplate, times(1)).expire(eq("rate_limit:/api/v1/shopping/compare:1.1.1.1"), eq(Duration.ofMinutes(1)));
    }

    @Test
    void 일일_한도를_넘으면_false() {
        assertThat(apiQuotaService.tryAcquireDaily("SERP", 2)).isTrue();
        assertThat(apiQuotaService.tryAcquireDaily("SERP", 2)).isTrue();
        assertThat(apiQuotaService.tryAcquireDaily("SERP", 2)).isFalse();
    }

    @Test
    void Redis_장애시에는_막지_않는다() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThat(interceptor.preHandle(request("1.1.1.1"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(apiQuotaService.tryAcquireDaily("SERP", 1)).isTrue();
        verify(redisTemplate, times(0)).expire(anyString(), any(Duration.class));
    }
}
