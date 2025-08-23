package com.notificationhub.dto;

import com.notificationhub.entity.WebhookEndpoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

public class WebhookDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String userId;

        @NotBlank
        private String name;

        @NotBlank
        private String url;

        private String secret;

        @NotNull
        private Set<WebhookEndpoint.WebhookEventType> subscribedEvents;

        private Integer maxRetries;

        private Integer timeoutSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String url;
        private String secret;
        private Set<WebhookEndpoint.WebhookEventType> subscribedEvents;
        private WebhookEndpoint.WebhookStatus status;
        private Integer maxRetries;
        private Integer timeoutSeconds;
    }

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
