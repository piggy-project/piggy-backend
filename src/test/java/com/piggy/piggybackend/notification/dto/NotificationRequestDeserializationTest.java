package com.piggy.piggybackend.notification.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRequestDeserializationTest {

    // Spring Boot 기본 ObjectMapper와 동일한 설정 (모르는 필드는 무시)
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void 퍼센트_알림_요청의_isActive가_true로_역직렬화된다() throws Exception {
        String json = "{\"currencyCode\":\"USD\",\"targetPercent\":1.0,\"period\":\"DAILY\",\"isActive\":true}";

        NotificationSettingRequest request = objectMapper.readValue(json, NotificationSettingRequest.class);

        assertThat(request.isActive()).isTrue();
    }

    @Test
    void 목표가_알림_요청의_isActive가_true로_역직렬화된다() throws Exception {
        String json = "{\"currencyCode\":\"USD\",\"targetPrice\":1300.0,\"direction\":\"BELOW\",\"isActive\":true}";

        TargetPriceSettingRequest request = objectMapper.readValue(json, TargetPriceSettingRequest.class);

        assertThat(request.isActive()).isTrue();
    }

    @Test
    void active_키로_보내도_true로_역직렬화된다() throws Exception {
        String percentJson = "{\"currencyCode\":\"USD\",\"targetPercent\":1.0,\"period\":\"DAILY\",\"active\":true}";
        String targetJson = "{\"currencyCode\":\"USD\",\"targetPrice\":1300.0,\"direction\":\"BELOW\",\"active\":true}";

        assertThat(objectMapper.readValue(percentJson, NotificationSettingRequest.class).isActive()).isTrue();
        assertThat(objectMapper.readValue(targetJson, TargetPriceSettingRequest.class).isActive()).isTrue();
    }
}
