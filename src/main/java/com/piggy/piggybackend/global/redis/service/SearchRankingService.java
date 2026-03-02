package com.piggy.piggybackend.global.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchRankingService {

    private final StringRedisTemplate redisTemplate;
    private static final String RANKING_KEY_PREFIX = "search_ranking:";

    // 검색어 카운트 증가 (ZINCRBY)
    public void incrementKeywordCount(String keyword, String currencyCode) {
        // 1. 해당 국가 전용 랭킹 (예: search_ranking:JPY)
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY_PREFIX + currencyCode, keyword, 1);

        // 2. 전체 통합 랭킹 (예: search_ranking:ALL) - Fallback용
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY_PREFIX + "ALL", keyword, 1);
    }

    // 상위 N개 키워드 조회 (ZREVRANGE)
    public List<String> getTopKeywords(String currencyCode, int limit) {
        String key = RANKING_KEY_PREFIX + currencyCode;

        // Redis에서 점수가 높은 순으로 조회
        Set<String> range = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);

        if (range == null || range.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(range);
    }
}