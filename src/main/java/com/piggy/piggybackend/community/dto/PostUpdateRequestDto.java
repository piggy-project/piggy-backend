package com.piggy.piggybackend.community.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private String code;        // 통화 코드 수정 가능
    private MultipartFile image; // 새로운 이미지 (있을 경우)
    private boolean removeImage; // 기존 이미지를 삭제만 하고 싶을 때 사용
}