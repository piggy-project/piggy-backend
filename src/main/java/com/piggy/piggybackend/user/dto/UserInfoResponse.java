package com.piggy.piggybackend.user.dto;

import com.piggy.piggybackend.user.entity.User;
import lombok.Getter;

@Getter
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private String name;
    private String profileImageUrl;

    // Entity -> DTO 변환 생성자
    public UserInfoResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.name = user.getName();
        this.profileImageUrl = user.getProfileImageUrl();
    }
}