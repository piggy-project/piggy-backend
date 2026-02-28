package com.piggy.piggybackend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
//소셜 로그인 추가 정보 입력 dto
public class AdditionalInfoRequest {
    private String nickname;
    private String birth;
    private String phoneNum;
}
