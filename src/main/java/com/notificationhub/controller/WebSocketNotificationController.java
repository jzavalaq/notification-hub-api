package com.notificationhub.controller;

import com.notificationhub.dto.NotificationMessageDto;
import com.notificationhub.dto.RealTimeNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket controller for real-time notifications.
 *
 * <p>Message mappings:
 * <ul>
 *   <li>/app/notify.send/{userId} - Send notification to specific user</li>
 *   <li>/app/notify.broadcast - Broadcast to all connected users</li>
 *   <li>/app/notify.typing - Send typing indicator</li>
 *   <li>/app/presence - Update user presence status</li>
 * </ul>
 *
 * <p>Subscription destinations:
 * <ul>
 *   <li>/user/queue/notifications - User-specific notifications</li>
 *   <li>/topic/broadcast - Broadcast notifications</li>
 *   <li>/topic/presence - User presence updates</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    // Track online users (in production, use Redis)
    private final Map<String, Instant> onlineUsers = new ConcurrentHashMap<>();

    /**
     * Send a real-time notification to a specific user.
     *
     * <p>Client usage:
     * <pre>
     * stompClient.send("/app/notify.send/john_doe", {}, JSON.stringify({
     *     type: 'ALERT',
     *     title: 'New Message',
     *     message: 'You have a new message',
     *     data: { messageId: 123 }
     * }));
     * </pre>
     */
    @MessageMapping("/notify.send/{userId}")
    public void sendToUser(
            @DestinationVariable String userId,
            @Payload NotificationMessageDto notification,
            Principal principal) {

        log.info("Sending notification to user {}: {}", userId, notification.getTitle());

        RealTimeNotificationDto realTimeNotification = RealTimeNotificationDto.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .fromUser(principal != null ? principal.getName() : "system")
                .timestamp(Instant.now())
                .build();

        // Send to specific user's queue
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                realTimeNotification
        );
    }

    /**
     * Broadcast a notification to all connected users.
     *
     * <p>Client usage:
     * <pre>
     * stompClient.send("/app/notify.broadcast", {}, JSON.stringify({
     *     type: 'SYSTEM',
     *     title: 'Maintenance Notice',
     *     message: 'System maintenance in 30 minutes'
     * }));
     * </pre>
     */
    @MessageMapping("/notify.broadcast")
    public void broadcast(@Payload NotificationMessageDto notification, Principal principal) {
        log.info("Broadcasting notification: {}", notification.getTitle());

        RealTimeNotificationDto realTimeNotification = RealTimeNotificationDto.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .fromUser(principal != null ? principal.getName() : "system")
                .timestamp(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/broadcast", realTimeNotification);
    }

    /**
     * Send typing indicator to a specific user.
     *
     * <p>Client usage:
     * <pre>
     * stompClient.send("/app/notify.typing", {}, JSON.stringify({
     *     toUser: 'john_doe',
     *     isTyping: true,
     *     conversationId: 'conv-123'
     * }));
     * </pre>
     */
    @MessageMapping("/notify.typing")
    public void typingIndicator(@Payload Map<String, Object> typingData, Principal principal) {
        String toUser = (String) typingData.get("toUser");
        Boolean isTyping = (Boolean) typingData.get("isTyping");

        if (toUser != null && principal != null) {
            messagingTemplate.convertAndSendToUser(
                    toUser,
                    "/queue/typing",
                    Map.of(
                            "fromUser", principal.getName(),
                            "isTyping", isTyping != null ? isTyping : false,
                            "timestamp", Instant.now()
                    )
            );
        }
    }

    /**
     * Update user presence status.
     *
     * <p>Client usage:
     * <pre>
     * stompClient.send("/app/presence", {}, JSON.stringify({
     *     status: 'ONLINE',
     *     message: 'Available'
     * }));
     * </pre>
     */
    @MessageMapping("/presence")
    public void updatePresence(@Payload Map<String, Object> presenceData, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            onlineUsers.put(username, Instant.now());

            // Broadcast presence update
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "user", username,
                    "status", presenceData.getOrDefault("status", "ONLINE"),
                    "message", presenceData.getOrDefault("message", ""),
                    "timestamp", Instant.now()
            ));
        }
    }

    /**
     * Handle user subscription to notification channel.
     * Registers the user as online.
     */
    @MessageMapping("/presence.subscribe")
    public void subscribePresence(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            onlineUsers.put(username, Instant.now());
            log.info("User {} is now online", username);

            // Notify others of user's online status
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "user", username,
                    "status", "ONLINE",
                    "timestamp", Instant.now()
            ));
        }
    }

    /**
     * Get list of online users.
     */
    public Map<String, Instant> getOnlineUsers() {
        return new ConcurrentHashMap<>(onlineUsers);
    }
}
