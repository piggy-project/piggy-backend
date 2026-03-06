package com.piggy.piggybackend.shopping.util.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regional_foods")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RegionalFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String countryCode; // 예: JPN, VNM, USA

    @Column(name = "food_rank", nullable = false)
    private Integer rank; // 해당 국가 내 순위

    @Column(nullable = false)
    private String name; // 음식 이름 (영문/현지명)

    private String category; // 음식 분류 (Noodles, Rice Dish 등)

    private Double rating; // TasteAtlas 기반 평점

    @Column(columnDefinition = "TEXT")
    private String description; // 음식에 대한 한글 설명 (desc)

    // 업데이트가 필요할 경우를 위한 편의 메서드
    public void update(Integer rank, String name, String category, Double rating, String description) {
        this.rank = rank;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.description = description;
    }
}