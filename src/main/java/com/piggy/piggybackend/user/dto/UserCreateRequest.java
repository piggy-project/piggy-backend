package com.piggy.piggybackend.user.dto;

import com.piggy.piggybackend.user.entity.Provider;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserCreateRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank
    @Size(max = 128)
    private String email;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    @NotBlank
    private String checkedPassword;

    @NotBlank
    @Size(min = 2, max = 20)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    @NotNull
    private LocalDate birth;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10,11}$",
            message = "전화번호는 하이픈(-) 없이 10~11자리 숫자여야 합니다.")
    private String phoneNum;
    @NotNull
    private Provider provider; // 소셜로그인
    private String providerId; // 소셜 고유 ID
}
