package com.piggy.piggybackend.auth.dto;

public interface OAuth2UserInfo {

    String getProviderId();
    String getEmail();
    String getName();
}