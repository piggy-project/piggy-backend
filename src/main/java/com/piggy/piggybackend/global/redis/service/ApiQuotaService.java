package com.piggy.piggybackend.global.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiQuotaService {

    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String DAILY_QUOTA_PREFIX = "api_quota:";

    // 고정 윈도우 카운터 (INCR + EXPIRE): 윈도우 내 limit 초과 시 false
    public boolean tryAcquire(String key, int limit, Duration window) {
        return increment(RATE_LIMIT_PREFIX + key, window) <= limit;
    }

    // 외부 API 일일 호출 한도 (예: api_quota:SERP:2026-10-16)
    public boolean tryAcquireDaily(String apiName, int dailyLimit) {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
        long count = increment(DAILY_QUOTA_PREFIX + apiName + ":" + today, Duration.ofDays(1));
        if (count > dailyLimit) {
            log.warn("[{}] 일일 호출 한도 초과 ({}/{})", apiName, count, dailyLimit);
            return false;
        }
        return true;
    }

    private long increment(String key, Duration ttl) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, ttl);
            }
            return count == null ? 0 : count;
        } catch (Exception e) {
            // Redis 장애 시 서비스 자체가 막히지 않도록 통과시킴
            log.error("Rate limit 카운터 처리 에러 - key: {}", key, e);
            return 0;
        }
    }
}
