package com.piggy.piggybackend.community.dto;

import com.piggy.piggybackend.community.entity.BoardCategory;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequestDto {
    private String title;
    private String content;
    private BoardCategory category;
    private String code;
    private MultipartFile image;
}
