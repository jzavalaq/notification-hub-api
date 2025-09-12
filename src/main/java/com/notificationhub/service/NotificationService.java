package com.notificationhub.service;

import com.notificationhub.dto.NotificationDto;
import com.notificationhub.dto.PageResponse;
import com.notificationhub.entity.*;
import com.notificationhub.exception.NotificationException;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.NotificationAuditRepository;
import com.notificationhub.repository.NotificationRepository;
import com.notificationhub.repository.NotificationTemplateRepository;
import com.notificationhub.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for sending and managing notifications.
 * <p>
 * Handles notification creation, delivery across multiple channels,
 * status tracking, and retry logic for failed notifications.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationAuditRepository auditRepository;
    private final PreferenceService preferenceService;
    private final TemplateService templateService;
    private final WebhookService webhookService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public NotificationDto.Response sendNotification(NotificationDto.SendRequest request) {
        NotificationTemplate template = null;
        String content = request.getContent();
        String subject = request.getSubject();

        // If template code provided, render content
        if (request.getTemplateCode() != null) {
            template = templateRepository.findByCode(request.getTemplateCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Template", "code", request.getTemplateCode()));

            content = templateService.renderTemplate(template, request.getTemplateVariables());
            subject = templateService.renderSubject(template, request.getTemplateVariables());
        }

        if (content == null || content.isBlank()) {
            throw new NotificationException("Notification content is required");
        }

        // Check user preferences
        NotificationPreference preference = preferenceService.findByUserId(request.getUserId()).orElse(null);
        if (preference != null) {
            if (preferenceService.isOptedOut(preference, request.getNotificationType())) {
                throw new NotificationException("User has opted out of this notification type");
            }
            if (!preferenceService.isChannelEnabled(preference, request.getChannel())) {
                throw new NotificationException("Channel is disabled for this user");
            }
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .notificationType(request.getNotificationType())
                .channel(request.getChannel())
                .recipient(request.getRecipient())
                .subject(subject)
                .content(content)
                .status(request.getScheduledAt() != null ? Notification.NotificationStatus.PENDING : Notification.NotificationStatus.QUEUED)
                .template(template)
                .metadata(request.getMetadata() != null ? request.getMetadata() : Map.of())
                .priority(request.getPriority() != null ? request.getPriority() :
                        (preference != null ? preference.getDefaultPriority() : NotificationPreference.Priority.NORMAL))
                .scheduledAt(request.getScheduledAt())
                .maxRetries(AppConstants.DEFAULT_MAX_RETRIES)
                .retryCount(0)
                .build();

        notification = notificationRepository.save(notification);

        // Create audit record
        createAuditRecord(notification, NotificationAudit.AuditAction.CREATED, null, notification.getStatus().name());

        // If not scheduled, process immediately
        if (request.getScheduledAt() == null) {
            processNotificationAsync(notification);
        }

        log.info("Created notification {} for user {}", notification.getId(), request.getUserId());
        return toResponse(notification);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> processNotificationAsync(Notification notification) {
        try {
            processNotification(notification);
        } catch (Exception e) {
            log.error("Error processing notification {}: {}", notification.getId(), e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void processNotification(Notification notification) {
        try {
            // Simulate sending based on channel
            boolean success = sendToChannel(notification);

            if (success) {
                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                createAuditRecord(notification, NotificationAudit.AuditAction.SENT, null, notification.getStatus().name());

                // Trigger webhooks
                webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT);

                // Send real-time notification via WebSocket
                webSocketNotificationService.sendToUser(
                    notification.getUserId(),
                    "NOTIFICATION",
                    notification.getSubject() != null ? notification.getSubject() : "New Notification",
                    notification.getContent()
                );
            } else {
                handleFailure(notification, "Failed to send to channel");
            }

            notificationRepository.save(notification);
        } catch (Exception e) {
            handleFailure(notification, e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private boolean sendToChannel(Notification notification) {
        // Simulate channel-specific sending logic
        switch (notification.getChannel()) {
            case EMAIL:
                return sendEmail(notification);
            case SMS:
                return sendSms(notification);
            case PUSH:
                return sendPush(notification);
            case IN_APP:
                return sendInApp(notification);
            default:
                return false;
        }
    }

    private boolean sendEmail(Notification notification) {
        log.info("Sending email to: {}", notification.getRecipient());
        // In real implementation, integrate with email service
        return true;
    }

    private boolean sendSms(Notification notification) {
        log.info("Sending SMS to: {}", notification.getRecipient());
        // In real implementation, integrate with Twilio or similar
        return true;
    }

    private boolean sendPush(Notification notification) {
        log.info("Sending push notification to: {}", notification.getRecipient());
        // In real implementation, integrate with FCM/APNs
        return true;
    }

    private boolean sendInApp(Notification notification) {
        log.info("Sending in-app notification to: {}", notification.getRecipient());
        return true;
    }

    private void handleFailure(Notification notification, String errorMessage) {
        notification.setErrorMessage(errorMessage);
        notification.setRetryCount(notification.getRetryCount() + 1);

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            createAuditRecord(notification, NotificationAudit.AuditAction.FAILED, errorMessage, notification.getStatus().name());
            webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_FAILED);
        } else {
            createAuditRecord(notification, NotificationAudit.AuditAction.RETRIED, errorMessage, notification.getStatus().name());
        }
    }

    @Transactional
    public List<NotificationDto.Response> sendBatchNotifications(NotificationDto.BatchSendRequest request) {
        // Use parallel processing for batch notifications
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(request.getRecipients().size(), AppConstants.MAX_BATCH_PARALLEL_THREADS));

        try {
            List<CompletableFuture<NotificationDto.Response>> futures = request.getRecipients().stream()
                    .map(recipient -> CompletableFuture.supplyAsync(() -> {
                        NotificationDto.SendRequest sendRequest = NotificationDto.SendRequest.builder()
                                .userId(recipient.getUserId())
                                .notificationType(request.getNotificationType())
                                .channel(request.getChannel())
                                .recipient(recipient.getRecipient())
                                .subject(request.getSubject())
                                .content(request.getContent())
                                .templateCode(request.getTemplateCode())
                                .templateVariables(mergeVariables(request.getTemplateVariables(), recipient.getVariables()))
                                .priority(request.getPriority())
                                .scheduledAt(request.getScheduledAt())
                                .build();

                        try {
                            return sendNotification(sendRequest);
                        } catch (Exception e) {
                            log.error("Failed to send notification to {}: {}", recipient.getRecipient(), e.getMessage());
                            return null;
                        }
                    }, executor))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(n -> n != null)
                    .collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
    }

    private Map<String, String> mergeVariables(Map<String, String> base, Map<String, String> override) {
        if (base == null && override == null) return Map.of();
        if (base == null) return override;
        if (override == null) return base;

        Map<String, String> merged = new java.util.HashMap<>(base);
        merged.putAll(override);
        return merged;
    }

    @Transactional
    public NotificationDto.Response markAsDelivered(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setStatus(Notification.NotificationStatus.DELIVERED);
        notification.setDeliveredAt(Instant.now());
        notification = notificationRepository.save(notification);

        createAuditRecord(notification, NotificationAudit.AuditAction.DELIVERED, null, notification.getStatus().name());
        webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_DELIVERED);

        // Send real-time status update via WebSocket
        webSocketNotificationService.sendToUser(
            notification.getUserId(),
            "DELIVERED",
            "Notification Delivered",
            "Your notification has been delivered"
        );

        return toResponse(notification);
    }

    @Transactional
    public NotificationDto.Response markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        createAuditRecord(notification, NotificationAudit.AuditAction.READ, null, notification.getStatus().name());
        webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_READ);

        // Send real-time status update via WebSocket
        webSocketNotificationService.sendToUser(
            notification.getUserId(),
            "READ",
            "Notification Read",
            "Your notification has been marked as read"
        );

        return toResponse(notification);
    }

    @Transactional
    @Scheduled(fixedRate = 60000) // Run every minute
    public void processScheduledNotifications() {
        List<Notification> scheduled = notificationRepository
                .findByStatusAndScheduledAtBefore(Notification.NotificationStatus.PENDING, Instant.now());

        for (Notification notification : scheduled) {
            notification.setStatus(Notification.NotificationStatus.QUEUED);
            notificationRepository.save(notification);
            processNotificationAsync(notification);
        }

        log.info("Processed {} scheduled notifications", scheduled.size());
    }

    @Transactional
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository
                .findByStatusAndRetryCountLessThan(Notification.NotificationStatus.FAILED, AppConstants.DEFAULT_MAX_RETRIES);

        for (Notification notification : failed) {
            notification.setStatus(Notification.NotificationStatus.QUEUED);
            notificationRepository.save(notification);
            processNotificationAsync(notification);
        }

        log.info("Retrying {} failed notifications", failed.size());
    }

    @Transactional(readOnly = true)
    public NotificationDto.Response getNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto.Response> getUserNotifications(String userId, int page, int size) {
        int safeSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (page < 0) page = AppConstants.DEFAULT_PAGE_NUMBER;
        if (safeSize < 1) safeSize = AppConstants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);

        List<NotificationDto.Response> content = notifications.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<NotificationDto.Response>builder()
                .content(content)
                .pageNumber(notifications.getNumber())
                .pageSize(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .first(notifications.isFirst())
                .last(notifications.isLast())
                .build();
    }

    private void createAuditRecord(Notification notification, NotificationAudit.AuditAction action, String details, String newStatus) {
        NotificationAudit audit = NotificationAudit.builder()
                .notification(notification)
                .action(action)
                .details(details)
                .newStatus(newStatus)
                .build();
        auditRepository.save(audit);
    }

    private NotificationDto.Response toResponse(Notification notification) {
        return NotificationDto.Response.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .status(notification.getStatus())
                .errorMessage(notification.getErrorMessage())
                .retryCount(notification.getRetryCount())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .templateId(notification.getTemplate() != null ? notification.getTemplate().getId() : null)
                .priority(notification.getPriority())
                .version(notification.getVersion())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
