package com.piggy.piggybackend.user.service;

import com.piggy.piggybackend.user.dto.FcmTokenRequestDto;
import com.piggy.piggybackend.user.entity.FcmToken;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.FcmTokenRepository;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveOrUpdateToken(User user, String tokenValue) {
        fcmTokenRepository.findByToken(tokenValue).ifPresentOrElse(
                existingToken -> {
                    if (!existingToken.getUser().getId().equals(user.getId())) {
                        fcmTokenRepository.delete(existingToken);
                        createNewToken(user, tokenValue);
                    } else {
                        existingToken.updateLastUsed();
                    }
                },
                () -> createNewToken(user, tokenValue)
        );
    }

    private void createNewToken(User user, String tokenValue) {
        FcmToken newToken = FcmToken.builder()
                .token(tokenValue)
                .user(user)
                .lastUsedAt(LocalDateTime.now())
                .build();
        fcmTokenRepository.save(newToken);
    }
}