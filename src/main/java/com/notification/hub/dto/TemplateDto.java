package com.notification.hub.dto;

import com.notification.hub.entity.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Objects for template operations.
 */
public class TemplateDto {

    /**
     * Request DTO for creating a notification template.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Template code is required")
        @Size(min = 2, max = 100, message = "Code must be between 2 and 100 characters")
        private String code;

        @NotBlank(message = "Template name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;

        private String subject;

        @NotBlank(message = "Template body is required")
        private String body;

        @NotNull(message = "Channel is required")
        private NotificationTemplate.ChannelType channel;

        private String language;
    }

    /**
     * Request DTO for updating a notification template.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;
        private String subject;
        private String body;
        private NotificationTemplate.ChannelType channel;
        private String language;
        private NotificationTemplate.TemplateStatus status;
    }

    /**
     * Response DTO for template operations.
     */
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
