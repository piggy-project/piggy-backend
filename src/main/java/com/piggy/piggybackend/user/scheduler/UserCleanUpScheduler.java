package com.piggy.piggybackend.user.scheduler;

import com.piggy.piggybackend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanUpScheduler {

    private final UserService userService;

    // 매일 새벽 3시에 실행 (cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupDeletedUsers() {
        log.info("1달 경과 탈퇴 회원 영구 삭제 스케줄러 실행");
        userService.deleteExpiredUsers();
        log.info("1달 경과 탈퇴 회원 영구 삭제 완료");
    }
}