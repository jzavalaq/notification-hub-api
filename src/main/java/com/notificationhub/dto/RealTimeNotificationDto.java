package com.notificationhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for real-time WebSocket notifications sent to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeNotificationDto {

    private String id;

    private String type;

    private String title;

    private String message;

    private Map<String, Object> data;

    private String fromUser;

    private Instant timestamp;

    private boolean read;
}
