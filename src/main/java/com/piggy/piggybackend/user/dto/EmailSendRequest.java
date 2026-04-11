package com.piggy.piggybackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailSendRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank
    private String email;
}
