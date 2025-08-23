package com.notificationhub.dto;

import com.notificationhub.entity.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class TemplateDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String code;

        @NotBlank
        private String name;

        private String subject;

        @NotBlank
        private String body;

        @NotNull
        private NotificationTemplate.ChannelType channel;

        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String subject;
        private String body;
        private NotificationTemplate.ChannelType channel;
        private String language;
        private NotificationTemplate.TemplateStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String code;
        private String name;
        private String subject;
        private String body;
        private NotificationTemplate.ChannelType channel;
        private String language;
        private NotificationTemplate.TemplateStatus status;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
