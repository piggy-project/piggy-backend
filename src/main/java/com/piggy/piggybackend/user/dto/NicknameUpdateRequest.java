package com.piggy.piggybackend.user.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank(message = "닉네임은 공백일 수 없습니다.")
    @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다.")
    private String nickname;
}