package com.piggy.piggybackend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    // Expo 개발 서버 주소 (환경변수 APP_EXPO_HOST, 기본값 localhost:8081)
    @Value("${app.expo-host:localhost:8081}")
    private String expoHost;

    public void sendResetMail(String to, String token, String source) {
        String resetLink;


        if ("app".equalsIgnoreCase(source)) {
            // 앱 요청일 경우 (Expo Go 딥링크)
            resetLink = "exp://" + expoHost + "/--/password-reset?token=" + token;
        } else {
            // 웹 요청일 경우 (일반 HTTPS 링크)
            resetLink = "https://piggys.mooo.com/password/reset?token=" + token;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Piggy] 비밀번호 재설정 안내");
        message.setText("안녕하세요. 비밀번호 재설정을 위해 아래 링크를 클릭해주세요.\n\n" +
                "비밀번호 재설정 링크: " + resetLink + "\n\n" +
                "본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.");

        mailSender.send(message);
    }
}