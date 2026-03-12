package com.piggy.piggybackend.user.repository;

import com.piggy.piggybackend.user.entity.PasswordResetToken;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    @Modifying
    @Transactional
    void deleteAllByExpiryDateBefore(LocalDateTime now);

    // (참고) 아까 추가했던 유저별 삭제 로직
    @Modifying
    @Transactional
    void deleteByUser(User user);
}