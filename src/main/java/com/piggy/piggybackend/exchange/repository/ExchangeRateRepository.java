package com.piggy.piggybackend.exchange.repository;

import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    // 특정 날짜의 환율 데이터가 이미 있는지 확인용
    List<ExchangeRate> findAllBySearchDate(String searchDate);
    // 특정 통화의 특정 날짜 이전 가장 최근 데이터를 하나 가져옵니다. (어제 환율 찾기용)
    Optional<ExchangeRate> findTopByCurUnitAndSearchDateBeforeOrderBySearchDateDesc(String curUnit, String searchDate);
    // 특정 통화의 모든 기록을 날짜 오름차순(과거 -> 현재)으로 가져옵니다.
    List<ExchangeRate> findAllByCurUnitOrderBySearchDateAsc(String curUnit);

    // 특정 시점 이후의 최저가 환율 조회
    Optional<ExchangeRate> findTopByCurUnitAndSearchDateGreaterThanEqualOrderByDealBasRAsc(String curUnit, String startDate);

    // 랭킹 조회용: 전 통화의 가장 최근 전일 마감가를 한 번에 조회 (N+1 방지)
    @Query("""
        SELECT e FROM ExchangeRate e
        WHERE e.searchDate = (
            SELECT MAX(e2.searchDate) FROM ExchangeRate e2
            WHERE e2.curUnit = e.curUnit AND e2.searchDate < :today
        )
    """)
    List<ExchangeRate> findLatestBeforeDate(@Param("today") String today);
}
