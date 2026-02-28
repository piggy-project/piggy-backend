package com.piggy.piggybackend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetMail(String to, String token) {

        String resetLink = "https://piggys.moon.com/password/reset?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 재설정");
        message.setText("아래 링크 클릭:\n" + resetLink);

        mailSender.send(message);
    }
}