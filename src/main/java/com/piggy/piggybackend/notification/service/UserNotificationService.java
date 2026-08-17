package com.piggy.piggybackend.notification.service;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.notification.dto.UserNotificationResponse;
import com.piggy.piggybackend.notification.entity.NotificationType;
import com.piggy.piggybackend.notification.entity.UserNotification;
import com.piggy.piggybackend.notification.repository.UserNotificationRepository;
import com.piggy.piggybackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;

    @Transactional
    public UserNotification save(User user, NotificationType type, String title, String body, String currencyCode) {
        if (user == null) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }

        return userNotificationRepository.save(UserNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .currencyCode(currencyCode)
                .build());
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getMyNotifications(User user) {
        validateLogin(user);

        return userNotificationRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(UserNotificationResponse::new)
                .toList();
    }

    @Transactional
    public UserNotificationResponse markAsRead(User user, Long notificationId) {
        validateLogin(user);

        UserNotification notification = getOwnedNotification(user, notificationId);
        notification.markAsRead();
        return new UserNotificationResponse(notification);
    }

    @Transactional
    public void deleteNotification(User user, Long notificationId) {
        validateLogin(user);

        UserNotification notification = getOwnedNotification(user, notificationId);
        userNotificationRepository.delete(notification);
    }

    private UserNotification getOwnedNotification(User user, Long notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_AUTHORIZED);
        }

        return notification;
    }

    private void validateLogin(User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.LOGIN_REQUIRED);
        }
    }
}
