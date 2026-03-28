package com.notification.hub.controller;

import com.notification.hub.dto.NotificationDto;
import com.notification.hub.dto.PageResponse;
import com.notification.hub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for sending and managing notifications.
 * <p>
 * Supports single and batch notification sending across multiple channels
 * including email, SMS, push notifications, and in-app messaging.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification sending and management")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Sends a single notification.
     *
     * @param request the notification send request
     * @return the created notification
     */
    @PostMapping("/send")
    @Operation(summary = "Send a notification")
    @ApiResponse(responseCode = "201", description = "Notification sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<NotificationDto.Response> sendNotification(@Valid @RequestBody NotificationDto.SendRequest request) {
        log.info("Sending notification to user: {} via channel: {}", request.getUserId(), request.getChannel());
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    /**
     * Sends batch notifications to multiple recipients.
     *
     * @param request the batch send request
     * @return list of created notifications
     */
    @PostMapping("/batch")
    @Operation(summary = "Send batch notifications")
    @ApiResponse(responseCode = "201", description = "Batch notifications sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<List<NotificationDto.Response>> sendBatchNotifications(@Valid @RequestBody NotificationDto.BatchSendRequest request) {
        log.info("Sending batch notifications to {} recipients via channel: {}",
                request.getRecipients() != null ? request.getRecipients().size() : 0, request.getChannel());
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendBatchNotifications(request));
    }

    /**
     * Retrieves a notification by its ID.
     *
     * @param id the notification ID
     * @return the notification
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    @ApiResponse(responseCode = "200", description = "Notification found")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationDto.Response> getNotification(@PathVariable Long id) {
        log.debug("Fetching notification with id: {}", id);
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    /**
     * Retrieves notifications for a specific user with pagination.
     *
     * @param userId the user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of notifications
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    public ResponseEntity<PageResponse<NotificationDto.Response>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching notifications for user: {} - page: {}, size: {}", userId, page, size);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size));
    }

    /**
     * Marks a notification as delivered.
     *
     * @param id the notification ID
     * @return the updated notification
     */
    @PatchMapping("/{id}/deliver")
    @Operation(summary = "Mark notification as delivered")
    @ApiResponse(responseCode = "200", description = "Notification marked as delivered")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationDto.Response> markAsDelivered(@PathVariable Long id) {
        log.info("Marking notification {} as delivered", id);
        return ResponseEntity.ok(notificationService.markAsDelivered(id));
    }

    /**
     * Marks a notification as read.
     *
     * @param id the notification ID
     * @return the updated notification
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationDto.Response> markAsRead(@PathVariable Long id) {
        log.info("Marking notification {} as read", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
