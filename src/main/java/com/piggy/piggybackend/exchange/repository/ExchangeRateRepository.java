package com.piggy.piggybackend.exchange.repository;

import com.piggy.piggybackend.exchange.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
