package com.piggy.piggybackend.shopping.util.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequest {
    private String image;
    private String currency; // 프론트에서 보낸 'JPY', 'USD' 등
}