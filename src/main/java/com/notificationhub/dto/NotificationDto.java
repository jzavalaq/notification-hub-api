package com.notificationhub.dto;

import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

public class NotificationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        @NotBlank
        private String userId;

        @NotBlank
        private String notificationType;

        @NotNull
        private NotificationTemplate.ChannelType channel;

        @NotBlank
        private String recipient;

        private String subject;

        private String content;

        private String templateCode;

        private Map<String, String> templateVariables;

        private Map<String, String> metadata;

        private NotificationPreference.Priority priority;

        private Instant scheduledAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSendRequest {
        @NotBlank
        private String notificationType;

        @NotNull
        private NotificationTemplate.ChannelType channel;

        private String subject;

        private String content;

        private String templateCode;

        private Map<String, String> templateVariables;

        @NotNull
        private java.util.List<Recipient> recipients;

        private NotificationPreference.Priority priority;

        private Instant scheduledAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Recipient {
            private String userId;
            private String recipient;
            private Map<String, String> variables;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String userId;
        private String notificationType;
        private NotificationTemplate.ChannelType channel;
        private String recipient;
        private String subject;
        private String content;
        private Notification.NotificationStatus status;
        private String errorMessage;
        private Integer retryCount;
        private Instant scheduledAt;
        private Instant sentAt;
        private Instant deliveredAt;
        private Instant readAt;
        private Long templateId;
        private NotificationPreference.Priority priority;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
