package com.piggy.piggybackend.user.repository;

import com.piggy.piggybackend.user.entity.Provider;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<User> findByEmail(String email);
    void deleteByDeletedAtBefore(LocalDateTime dateTime);
    Optional<User> findByProviderAndProviderId(
            Provider provider,
            String providerId
    );
}
