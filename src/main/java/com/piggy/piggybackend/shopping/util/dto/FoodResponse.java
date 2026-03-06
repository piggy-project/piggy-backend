package com.piggy.piggybackend.shopping.util.dto;

import com.piggy.piggybackend.shopping.util.entity.RegionalFood;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FoodResponse {
    private Integer rank;
    private String name;
    private String category;
    private Double rating;
    private String desc;
    private String image;

    // Entity -> DTO 변환 생성자
    public FoodResponse(RegionalFood entity) {
        this.rank = entity.getRank();
        this.name = entity.getName();
        this.category = entity.getCategory();
        this.rating = entity.getRating();
        this.desc = entity.getDescription();
        this.image = entity.getImageUrl();
    }
}