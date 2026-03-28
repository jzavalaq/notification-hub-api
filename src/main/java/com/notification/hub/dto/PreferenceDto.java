package com.notification.hub.dto;

import com.notification.hub.entity.NotificationPreference;
import com.notification.hub.entity.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * Data Transfer Objects for user notification preference operations.
 * <p>
 * Contains request and response DTOs for creating, updating, and retrieving
 * user notification preferences including channel settings and quiet hours.
 * </p>
 */
public class PreferenceDto {

    /**
     * Request DTO for creating user notification preferences.
     * <p>
     * Required fields: userId. All other fields have sensible defaults.
     * </p>
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
     * <p>
     * All fields are optional - only provided fields will be updated.
     * </p>
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
     * <p>
     * Contains complete preference information including metadata.
     * </p>
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
