package com.piggy.piggybackend.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.piggy.piggybackend.notification.entity.NotificationType;
import com.piggy.piggybackend.notification.entity.UserNotification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserNotificationResponse {
    private final Long id;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String currencyCode;
    private final LocalDateTime createdAt;

    @JsonProperty("isRead")
    private final boolean read;

    public UserNotificationResponse(UserNotification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.body = notification.getBody();
        this.currencyCode = notification.getCurrencyCode();
        this.read = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }
}
