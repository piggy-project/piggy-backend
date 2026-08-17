package com.piggy.piggybackend.notification.repository;

import com.piggy.piggybackend.notification.entity.UserNotification;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findAllByUserOrderByCreatedAtDesc(User user);
}
