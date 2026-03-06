package com.piggy.piggybackend.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3; // S3Config에서 만든 Bean을 주입받습니다.

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file) {
        // 1. 파일 이름 중복 방지를 위해 UUID 생성
        String fileName = "food/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            // 2. 메타데이터 설정 (파일 크기 및 타입)
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 3. S3에 업로드
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata));

            log.info("S3 파일 업로드 성공: {}", fileName);

            // 4. 업로드된 파일의 전체 URL 반환
            return amazonS3.getUrl(bucket, fileName).toString();

        } catch (IOException e) {
            log.error("S3 파일 업로드 에러: {}", e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }

    // 파일 삭제 로직 (필요 시)
    public void deleteFile(String fileUrl) {
        String fileName = fileUrl.split(".com/")[1]; // URL에서 파일명만 추출
        amazonS3.deleteObject(bucket, fileName);
    }
}