package com.piggy.piggybackend.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component // 스프링 빈으로 등록하여 CryptoUtil을 주입받을 수 있게 함
public class FcmTokenConverter implements AttributeConverter<String, String> {

    private static CryptoUtil cryptoUtil;

    @Autowired
    public void setCryptoUtil(CryptoUtil cryptoUtil) {
        FcmTokenConverter.cryptoUtil = cryptoUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        try {
            return cryptoUtil.encrypt(attribute);
        } catch (Exception e) {
            throw new RuntimeException("FCM 토큰 암호화 중 오류 발생", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        try {
            return cryptoUtil.decrypt(dbData);
        } catch (Exception e) {
            throw new RuntimeException("FCM 토큰 복호화 중 오류 발생", e);
        }
    }
}