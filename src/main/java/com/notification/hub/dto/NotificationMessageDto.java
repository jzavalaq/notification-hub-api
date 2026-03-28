package com.notification.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for incoming WebSocket notification messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDto {

    @NotBlank(message = "Notification type is required")
    private String type; // ALERT, INFO, SUCCESS, WARNING, SYSTEM

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Map<String, Object> data;

    private String toUser; // For targeted notifications

    private String conversationId; // For chat-related notifications
}
