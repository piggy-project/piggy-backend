package com.piggy.piggybackend.shopping.util.repository;

import com.piggy.piggybackend.shopping.util.entity.RegionalFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionalFoodRepository extends JpaRepository<RegionalFood, Long> {

    // 특정 국가의 음식을 순위별로 정렬하여 조회
    List<RegionalFood> findByCountryCodeOrderByRankAsc(String countryCode);

    // 특정 국가의 상위 N개만 조회할 때 사용
    List<RegionalFood> findTop10ByCountryCodeOrderByRankAsc(String countryCode);
}
