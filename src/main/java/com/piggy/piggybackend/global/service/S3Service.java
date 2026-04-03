package com.piggy.piggybackend.global.service;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName) {
        // 파일 검증
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image")) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "올바른 이미지 파일이 아닙니다.");
        }

        String fileName = dirName + "/" + UUID.randomUUID() + ".jpg";

        try {
            // 1. 리사이징 (300x300)
            BufferedImage resizedImage = Thumbnails.of(file.getInputStream())
                    .size(1080, 1080)
                    .outputQuality(0.85)
                    .outputFormat("jpg")
                    .asBufferedImage();

            // 2. 메모리 내 바이트 변환
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", os);
            byte[] bytes = os.toByteArray();

            // 3. S3 업로드
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                var resource = s3Template.upload(bucket, fileName, is);
                return resource.getURL().toString();
            }
        } catch (IOException e) {
            log.error("S3 업로드 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");
        }
    }

    public void delete(String fileUrl) {
        try {
            String key = fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5);
            s3Template.deleteObject(bucket, key);
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패: {}", fileUrl);
        }
    }
}