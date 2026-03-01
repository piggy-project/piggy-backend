package com.piggy.piggybackend.shopping.util.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequest {
    private String image; // 프론트에서 보낸 Base64 이미지 데이터
}