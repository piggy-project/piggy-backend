package com.piggy.piggybackend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    // application.yml에 적은 firebase.config-path 값을 가져옵니다.
    @Value("${firebase.config-path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void init() {
        try {
            // 가져온 경로(도커 내부 경로)에서 JSON 파일을 읽습니다.
            FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("[FCM] Firebase App 초기화 완료");
            }
        } catch (IOException e) {
            log.error("[FCM] Firebase 초기화 에러: JSON 파일을 찾을 수 없거나 권한이 없습니다. 경로: {}", firebaseConfigPath);
        }
    }
}