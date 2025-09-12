package com.notificationhub.dto;

import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Objects for notification operations.
 */
public class NotificationDto {

    /**
     * Request DTO for sending a single notification.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        @NotBlank(message = "User ID is required")
        private String userId;

        @NotBlank(message = "Notification type is required")
        private String notificationType;

        @NotNull(message = "Channel is required")
        private NotificationTemplate.ChannelType channel;

        @NotBlank(message = "Recipient is required")
        private String recipient;

        private String subject;

        private String content;

        private String templateCode;

        private Map<String, String> templateVariables;

        private Map<String, String> metadata;

        private NotificationPreference.Priority priority;

        private Instant scheduledAt;
    }

    /**
     * Request DTO for sending batch notifications.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSendRequest {
        @NotBlank(message = "Notification type is required")
        private String notificationType;

        @NotNull(message = "Channel is required")
        private NotificationTemplate.ChannelType channel;

        private String subject;

        private String content;

        private String templateCode;

        private Map<String, String> templateVariables;

        @NotNull(message = "Recipients list is required")
        @Size(min = 1, max = 1000, message = "Recipients list must contain between 1 and 1000 recipients")
        @Valid
        private java.util.List<Recipient> recipients;

        private NotificationPreference.Priority priority;

        private Instant scheduledAt;

        /**
         * DTO for a single recipient in a batch request.
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Recipient {
            @NotBlank(message = "User ID is required")
            private String userId;

            @NotBlank(message = "Recipient address is required")
            private String recipient;

            private Map<String, String> variables;
        }
    }

    /**
     * Response DTO for notification operations.
     */
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
