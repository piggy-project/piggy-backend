package com.piggy.piggybackend.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequest {
    private String fcmToken;
}