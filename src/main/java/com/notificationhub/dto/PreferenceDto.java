package com.notificationhub.dto;

import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * Data Transfer Objects for user preference operations.
 */
public class PreferenceDto {

    /**
     * Request DTO for creating user notification preferences.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "User ID is required")
        private String userId;

        private Set<NotificationTemplate.ChannelType> enabledChannels;
        private Set<String> optedOutTypes;
        private LocalTime quietHoursStart;
        private LocalTime quietHoursEnd;
        private String timezone;
        private NotificationPreference.Priority defaultPriority;
    }

    /**
     * Request DTO for updating user notification preferences.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Set<NotificationTemplate.ChannelType> enabledChannels;
        private Set<String> optedOutTypes;
        private LocalTime quietHoursStart;
        private LocalTime quietHoursEnd;
        private String timezone;
        private NotificationPreference.Priority defaultPriority;
    }

    /**
     * Response DTO for user preference operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String userId;
        private Set<NotificationTemplate.ChannelType> enabledChannels;
        private Set<String> optedOutTypes;
        private LocalTime quietHoursStart;
        private LocalTime quietHoursEnd;
        private String timezone;
        private NotificationPreference.Priority defaultPriority;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
