package com.notification.hub.service;

import com.notification.hub.dto.PageResponse;
import com.notification.hub.dto.WebhookDto;
import com.notification.hub.entity.Notification;
import com.notification.hub.entity.WebhookDelivery;
import com.notification.hub.entity.WebhookEndpoint;
import com.notification.hub.exception.DuplicateResourceException;
import com.notification.hub.exception.ResourceNotFoundException;
import com.notification.hub.repository.WebhookDeliveryRepository;
import com.notification.hub.repository.WebhookEndpointRepository;
import com.notification.hub.util.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

/**
 * Service for managing webhook endpoints and deliveries.
 * <p>
 * Handles webhook CRUD operations, event triggering, delivery tracking,
 * and retry logic with exponential backoff.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookEndpointRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public WebhookDto.Response createWebhook(WebhookDto.CreateRequest request) {
        WebhookEndpoint webhook = WebhookEndpoint.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .url(request.getUrl())
                .secret(request.getSecret() != null ? request.getSecret() : generateSecret())
                .subscribedEvents(request.getSubscribedEvents())
                .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : AppConstants.DEFAULT_WEBHOOK_MAX_RETRIES)
                .timeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : AppConstants.DEFAULT_WEBHOOK_TIMEOUT_SECONDS)
                .status(WebhookEndpoint.WebhookStatus.ACTIVE)
                .build();

        webhook = webhookRepository.save(webhook);
        log.info("Created webhook {} for user {}", webhook.getId(), request.getUserId());

        return toResponse(webhook);
    }

    @Transactional
    public WebhookDto.Response updateWebhook(Long id, WebhookDto.UpdateRequest request) {
        WebhookEndpoint webhook = webhookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id));

        if (request.getName() != null) webhook.setName(request.getName());
        if (request.getUrl() != null) webhook.setUrl(request.getUrl());
        if (request.getSecret() != null) webhook.setSecret(request.getSecret());
        if (request.getSubscribedEvents() != null) webhook.setSubscribedEvents(request.getSubscribedEvents());
        if (request.getStatus() != null) webhook.setStatus(request.getStatus());
        if (request.getMaxRetries() != null) webhook.setMaxRetries(request.getMaxRetries());
        if (request.getTimeoutSeconds() != null) webhook.setTimeoutSeconds(request.getTimeoutSeconds());

        webhook = webhookRepository.save(webhook);
        log.info("Updated webhook {}", id);

        return toResponse(webhook);
    }

    @Transactional(readOnly = true)
    public WebhookDto.Response getWebhook(Long id) {
        WebhookEndpoint webhook = webhookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id));
        return toResponse(webhook);
    }

    /**
     * Retrieves all webhooks for a specific user with pagination.
     *
     * @param userId the user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of webhooks for the user
     */
    @Transactional(readOnly = true)
    public PageResponse<WebhookDto.Response> getUserWebhooks(String userId, int page, int size) {
        int safeSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (page < 0) page = AppConstants.DEFAULT_PAGE_NUMBER;
        if (safeSize < 1) safeSize = AppConstants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<WebhookEndpoint> webhooks = webhookRepository.findByUserId(userId, pageable);

        List<WebhookDto.Response> content = webhooks.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<WebhookDto.Response>builder()
                .content(content)
                .pageNumber(webhooks.getNumber())
                .pageSize(webhooks.getSize())
                .totalElements(webhooks.getTotalElements())
                .totalPages(webhooks.getTotalPages())
                .first(webhooks.isFirst())
                .last(webhooks.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<WebhookDto.Response> getAllWebhooks(int page, int size) {
        int safeSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (page < 0) page = AppConstants.DEFAULT_PAGE_NUMBER;
        if (safeSize < 1) safeSize = AppConstants.DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<WebhookEndpoint> webhooks = webhookRepository.findAll(pageable);

        List<WebhookDto.Response> content = webhooks.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<WebhookDto.Response>builder()
                .content(content)
                .pageNumber(webhooks.getNumber())
                .pageSize(webhooks.getSize())
                .totalElements(webhooks.getTotalElements())
                .totalPages(webhooks.getTotalPages())
                .first(webhooks.isFirst())
                .last(webhooks.isLast())
                .build();
    }

    @Transactional
    public void deleteWebhook(Long id) {
        if (!webhookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Webhook", "id", id);
        }
        webhookRepository.deleteById(id);
        log.info("Deleted webhook {}", id);
    }

    @Async
    public CompletableFuture<Void> triggerWebhooks(Notification notification, WebhookEndpoint.WebhookEventType eventType) {
        List<WebhookEndpoint> webhooks = webhookRepository
                .findBySubscribedEventsContaining(eventType).stream()
                .filter(w -> w.getStatus() == WebhookEndpoint.WebhookStatus.ACTIVE)
                .collect(Collectors.toList());

        for (WebhookEndpoint webhook : webhooks) {
            try {
                deliverWebhook(webhook, notification, eventType);
            } catch (Exception e) {
                log.error("Failed to trigger webhook {}: {}", webhook.getId(), e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> deliverWebhookAsync(WebhookEndpoint webhook, Notification notification, WebhookEndpoint.WebhookEventType eventType) {
        String payload;
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("event", eventType.name());
            payloadMap.put("notificationId", notification.getId());
            payloadMap.put("userId", notification.getUserId());
            payloadMap.put("channel", notification.getChannel().name());
            payloadMap.put("status", notification.getStatus().name());
            payloadMap.put("timestamp", Instant.now().toString());
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }

        WebhookDelivery delivery = WebhookDelivery.builder()
                .webhook(webhook)
                .notification(notification)
                .eventType(eventType)
                .payload(payload)
                .status(WebhookDelivery.DeliveryStatus.PENDING)
                .attemptCount(0)
                .build();

        delivery = deliveryRepository.save(delivery);
        final WebhookDelivery finalDelivery = delivery;
        final String finalPayload = payload;

        String signature = generateSignature(payload, webhook.getSecret());

        WebClient client = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(AppConstants.WEBHOOK_MAX_IN_MEMORY_SIZE))
                .build();

        return client.post()
                .uri(webhook.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Signature", signature)
                .header("X-Webhook-Event", eventType.name())
                .body(Mono.just(payload), String.class)
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(webhook.getTimeoutSeconds()))
                .map(response -> response.getStatusCode().value())
                .onErrorReturn(0)
                .toFuture()
                .thenAccept(statusCode -> {
                    if (statusCode >= 200 && statusCode < 300) {
                        finalDelivery.setStatus(WebhookDelivery.DeliveryStatus.DELIVERED);
                        finalDelivery.setStatusCode(statusCode);
                        finalDelivery.setDeliveredAt(Instant.now());
                        log.info("Webhook {} delivered successfully", webhook.getId());
                    } else {
                        handleDeliveryFailure(finalDelivery, statusCode, "HTTP error");
                    }
                    deliveryRepository.save(finalDelivery);
                })
                .exceptionally(ex -> {
                    handleDeliveryFailure(finalDelivery, 0, ex.getMessage());
                    deliveryRepository.save(finalDelivery);
                    return null;
                });
    }

    @Transactional
    public void deliverWebhook(WebhookEndpoint webhook, Notification notification, WebhookEndpoint.WebhookEventType eventType) {
        // Delegate to async method for non-blocking delivery
        deliverWebhookAsync(webhook, notification, eventType);
    }

    private void handleDeliveryFailure(WebhookDelivery delivery, int statusCode, String errorMessage) {
        delivery.setStatusCode(statusCode);
        delivery.setErrorMessage(errorMessage);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        WebhookEndpoint webhook = delivery.getWebhook();
        if (delivery.getAttemptCount() >= webhook.getMaxRetries()) {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.FAILED);
            log.error("Webhook delivery failed after {} attempts: {}", delivery.getAttemptCount(), errorMessage);
        } else {
            delivery.setStatus(WebhookDelivery.DeliveryStatus.RETRYING);
            // Exponential backoff using predefined delays
            int attemptIndex = Math.min(delivery.getAttemptCount() - 1, AppConstants.WEBHOOK_RETRY_BACKOFF_MINUTES.length - 1);
            long delayMinutes = AppConstants.WEBHOOK_RETRY_BACKOFF_MINUTES[attemptIndex];
            delivery.setNextRetryAt(Instant.now().plusSeconds(delayMinutes * 60));
            log.warn("Webhook delivery failed, will retry in {} minutes", delayMinutes);
        }
    }

    @Transactional(readOnly = true)
    public void processRetries() {
        List<WebhookDelivery> pending = deliveryRepository
                .findByStatusAndNextRetryAtBefore(WebhookDelivery.DeliveryStatus.RETRYING, Instant.now());

        for (WebhookDelivery delivery : pending) {
            try {
                deliverWebhook(delivery.getWebhook(), delivery.getNotification(), delivery.getEventType());
            } catch (Exception e) {
                log.error("Failed to retry webhook delivery {}: {}", delivery.getId(), e.getMessage());
            }
        }

        log.info("Processed {} webhook retries", pending.size());
    }

    public boolean verifySignature(String payload, String signature, String secret) {
        String expectedSignature = generateSignature(payload, secret);
        return expectedSignature.equals(signature);
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to generate signature: {}", e.getMessage());
            return "";
        }
    }

    private String generateSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private WebhookDto.Response toResponse(WebhookEndpoint webhook) {
        return WebhookDto.Response.builder()
                .id(webhook.getId())
                .userId(webhook.getUserId())
                .name(webhook.getName())
                .url(webhook.getUrl())
                .subscribedEvents(webhook.getSubscribedEvents())
                .status(webhook.getStatus())
                .maxRetries(webhook.getMaxRetries())
                .timeoutSeconds(webhook.getTimeoutSeconds())
                .version(webhook.getVersion())
                .createdAt(webhook.getCreatedAt())
                .updatedAt(webhook.getUpdatedAt())
                .build();
    }
}
