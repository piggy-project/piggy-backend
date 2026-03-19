package com.piggy.piggybackend.user.repository;

import com.piggy.piggybackend.user.entity.FcmToken;
import com.piggy.piggybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    // 토큰 문자열로 특정 기기 토큰 찾기
    Optional<FcmToken> findByToken(String token);

    // 특정 유저에게 연결된 모든 토큰 찾기 (알림 발송용)
    List<FcmToken> findAllByUser(User user);

    // 특정 토큰 삭제 (로그아웃 시 사용)
    void deleteByToken(String token);
}