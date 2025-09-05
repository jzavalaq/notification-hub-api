package com.notificationhub.service;

import com.notificationhub.dto.RealTimeNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending real-time notifications via WebSocket.
 *
 * <p>Usage examples:
 * <pre>
 * // Send to specific user
 * webSocketNotificationService.sendToUser("john_doe", "ALERT", "New Order", "You have a new order");
 *
 * // Broadcast to all users
 * webSocketNotificationService.broadcast("SYSTEM", "Maintenance", "System will restart in 5 min");
 *
 * // Send with additional data
 * webSocketNotificationService.sendToUser("john_doe", "CHAT", "New Message", "Hello!", Map.of("conversationId", "123"));
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a notification to a specific user.
     *
     * @param userId  the target user ID
     * @param type    notification type (ALERT, INFO, SUCCESS, WARNING, SYSTEM, CHAT)
     * @param title   notification title
     * @param message notification message
     */
    public void sendToUser(String userId, String type, String title, String message) {
        sendToUser(userId, type, title, message, null);
    }

    /**
     * Send a notification to a specific user with additional data.
     *
     * @param userId  the target user ID
     * @param type    notification type
     * @param title   notification title
     * @param message notification message
     * @param data    additional data payload
     */
    public void sendToUser(String userId, String type, String title, String message, Map<String, Object> data) {
        log.debug("Sending WebSocket notification to user {}: {}", userId, title);

        RealTimeNotificationDto notification = RealTimeNotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .read(false)
                .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }

    /**
     * Send a notification from a specific user to another user.
     *
     * @param toUser   the target user ID
     * @param fromUser the sender user ID
     * @param type     notification type
     * @param title    notification title
     * @param message  notification message
     */
    public void sendFromUser(String toUser, String fromUser, String type, String title, String message) {
        sendFromUser(toUser, fromUser, type, title, message, null);
    }

    /**
     * Send a notification from a specific user with additional data.
     */
    public void sendFromUser(String toUser, String fromUser, String type, String title, String message, Map<String, Object> data) {
        log.debug("Sending WebSocket notification from {} to {}: {}", fromUser, toUser, title);

        RealTimeNotificationDto notification = RealTimeNotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .fromUser(fromUser)
                .timestamp(Instant.now())
                .read(false)
                .build();

        messagingTemplate.convertAndSendToUser(toUser, "/queue/notifications", notification);
    }

    /**
     * Broadcast a notification to all connected users.
     *
     * @param type    notification type
     * @param title   notification title
     * @param message notification message
     */
    public void broadcast(String type, String title, String message) {
        broadcast(type, title, message, null);
    }

    /**
     * Broadcast a notification to all connected users with additional data.
     */
    public void broadcast(String type, String title, String message, Map<String, Object> data) {
        log.info("Broadcasting WebSocket notification: {}", title);

        RealTimeNotificationDto notification = RealTimeNotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .fromUser("system")
                .timestamp(Instant.now())
                .read(false)
                .build();

        messagingTemplate.convertAndSend("/topic/broadcast", notification);
    }

    /**
     * Send a typing indicator to a user.
     *
     * @param toUser   the target user
     * @param fromUser the user who is typing
     * @param isTyping whether the user is typing
     */
    public void sendTypingIndicator(String toUser, String fromUser, boolean isTyping) {
        messagingTemplate.convertAndSendToUser(toUser, "/queue/typing", Map.of(
                "fromUser", fromUser,
                "isTyping", isTyping,
                "timestamp", Instant.now()
        ));
    }

    /**
     * Notify about user presence change.
     *
     * @param userId   the user whose presence changed
     * @param status   the new status (ONLINE, OFFLINE, AWAY)
     * @param message  optional status message
     */
    public void notifyPresence(String userId, String status, String message) {
        messagingTemplate.convertAndSend("/topic/presence", Map.of(
                "user", userId,
                "status", status,
                "message", message != null ? message : "",
                "timestamp", Instant.now()
        ));
    }

    /**
     * Send a system alert to a specific user.
     */
    public void sendAlert(String userId, String title, String message) {
        sendToUser(userId, "ALERT", title, message);
    }

    /**
     * Send a success notification to a specific user.
     */
    public void sendSuccess(String userId, String title, String message) {
        sendToUser(userId, "SUCCESS", title, message);
    }

    /**
     * Send a warning notification to a specific user.
     */
    public void sendWarning(String userId, String title, String message) {
        sendToUser(userId, "WARNING", title, message);
    }

    /**
     * Send an info notification to a specific user.
     */
    public void sendInfo(String userId, String title, String message) {
        sendToUser(userId, "INFO", title, message);
    }
}
