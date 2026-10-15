package com.piggy.piggybackend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetMail(String to, String token, String source, String redirectUrl) {
        String resetLink;


        if ("app".equalsIgnoreCase(source) && isAllowedAppUrl(redirectUrl)) {
            // 앱 요청일 경우: 앱이 보낸 딥링크 주소(현재 연결된 와이파이 기준)를 그대로 사용
            resetLink = redirectUrl + "?token=" + token;
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

    // 앱 딥링크 스킴만 허용 (개발: exp://, 배포: piggy://)
    private boolean isAllowedAppUrl(String url) {
        return url != null && (url.startsWith("exp://") || url.startsWith("piggy://"));
    }
}