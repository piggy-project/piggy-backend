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
    private static final String RANKING_KEY = "search_ranking";

    // 검색어 카운트 증가 (ZINCRBY)
    public void incrementKeywordCount(String keyword) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1);
    }

    // 상위 N개 키워드 조회 (ZREVRANGE)
    public List<String> getTopKeywords(int n) {
        Set<String> range = redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, n - 1);
        return range != null ? new ArrayList<>(range) : new ArrayList<>();
    }
}