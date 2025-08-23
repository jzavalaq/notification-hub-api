package com.notificationhub.controller;

import com.notificationhub.dto.NotificationDto;
import com.notificationhub.dto.PageResponse;
import com.notificationhub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification sending and management")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @Operation(summary = "Send a notification")
    public ResponseEntity<NotificationDto.Response> sendNotification(@Valid @RequestBody NotificationDto.SendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    @PostMapping("/batch")
    @Operation(summary = "Send batch notifications")
    public ResponseEntity<List<NotificationDto.Response>> sendBatchNotifications(@Valid @RequestBody NotificationDto.BatchSendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendBatchNotifications(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationDto.Response> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications")
    public ResponseEntity<PageResponse<NotificationDto.Response>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size));
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Mark notification as delivered")
    public ResponseEntity<NotificationDto.Response> markAsDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsDelivered(id));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationDto.Response> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
