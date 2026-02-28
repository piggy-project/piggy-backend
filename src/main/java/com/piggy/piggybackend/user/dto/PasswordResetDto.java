package com.piggy.piggybackend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetDto {

    private String token;
    private String newPassword;
}