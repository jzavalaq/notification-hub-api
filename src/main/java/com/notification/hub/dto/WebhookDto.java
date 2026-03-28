package com.notification.hub.dto;

import com.notification.hub.entity.WebhookEndpoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Data Transfer Objects for webhook operations.
 */
public class WebhookDto {

    /**
     * Request DTO for creating a webhook endpoint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "User ID is required")
        private String userId;

        @NotBlank(message = "Webhook name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;

        @NotBlank(message = "Webhook URL is required")
        private String url;

        private String secret;

        @NotNull(message = "Subscribed events are required")
        @Size(min = 1, message = "At least one event type must be subscribed")
        private Set<WebhookEndpoint.WebhookEventType> subscribedEvents;

        private Integer maxRetries;

        private Integer timeoutSeconds;
    }

    /**
     * Request DTO for updating a webhook endpoint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;
        private String url;
        private String secret;
        private Set<WebhookEndpoint.WebhookEventType> subscribedEvents;
        private WebhookEndpoint.WebhookStatus status;
        private Integer maxRetries;
        private Integer timeoutSeconds;
    }

    /**
     * Response DTO for webhook operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String userId;
        private String name;
        private String url;
        private Set<WebhookEndpoint.WebhookEventType> subscribedEvents;
        private WebhookEndpoint.WebhookStatus status;
        private Integer maxRetries;
        private Integer timeoutSeconds;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
