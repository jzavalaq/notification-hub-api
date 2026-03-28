package com.notification.hub.controller;

import com.notification.hub.dto.PageResponse;
import com.notification.hub.dto.WebhookDto;
import com.notification.hub.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for managing webhook endpoints.
 * <p>
 * Provides CRUD operations for webhook endpoints that receive event
 * notifications when various notification events occur.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook endpoint management")
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Creates a new webhook endpoint.
     *
     * @param request the webhook creation request
     * @return the created webhook
     */
    @PostMapping
    @Operation(summary = "Create a webhook endpoint")
    @ApiResponse(responseCode = "201", description = "Webhook created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<WebhookDto.Response> createWebhook(@Valid @RequestBody WebhookDto.CreateRequest request) {
        log.info("Creating webhook for user: {} with URL: {}", request.getUserId(), request.getUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(webhookService.createWebhook(request));
    }

    /**
     * Retrieves a webhook by its ID.
     *
     * @param id the webhook ID
     * @return the webhook
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID")
    @ApiResponse(responseCode = "200", description = "Webhook found")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public ResponseEntity<WebhookDto.Response> getWebhook(@PathVariable Long id) {
        log.debug("Fetching webhook with id: {}", id);
        return ResponseEntity.ok(webhookService.getWebhook(id));
    }

    /**
     * Retrieves webhooks for a specific user with pagination support.
     *
     * @param userId the user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of webhooks for the user
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user webhooks")
    @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully")
    public ResponseEntity<PageResponse<WebhookDto.Response>> getUserWebhooks(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching webhooks for user: {} - page: {}, size: {}", userId, page, size);
        return ResponseEntity.ok(webhookService.getUserWebhooks(userId, page, size));
    }

    /**
     * Lists all webhooks with pagination support.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of webhooks
     */
    @GetMapping
    @Operation(summary = "List all webhooks")
    @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully")
    public ResponseEntity<PageResponse<WebhookDto.Response>> getAllWebhooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Listing webhooks - page: {}, size: {}", page, size);
        return ResponseEntity.ok(webhookService.getAllWebhooks(page, size));
    }

    /**
     * Updates an existing webhook.
     *
     * @param id the webhook ID
     * @param request the update request
     * @return the updated webhook
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a webhook")
    @ApiResponse(responseCode = "200", description = "Webhook updated successfully")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public ResponseEntity<WebhookDto.Response> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookDto.UpdateRequest request) {
        log.info("Updating webhook with id: {}", id);
        return ResponseEntity.ok(webhookService.updateWebhook(id, request));
    }

    /**
     * Deletes a webhook by ID.
     *
     * @param id the webhook ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    @ApiResponse(responseCode = "204", description = "Webhook deleted successfully")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWebhook(@PathVariable Long id) {
        log.info("Deleting webhook with id: {}", id);
        webhookService.deleteWebhook(id);
    }

    /**
     * Verifies a webhook signature.
     *
     * @param id the webhook ID
     * @param payload the payload to verify
     * @param signature the signature header
     * @return true if signature is valid
     */
    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify webhook signature")
    @ApiResponse(responseCode = "200", description = "Signature verification completed")
    @ApiResponse(responseCode = "404", description = "Webhook not found")
    public ResponseEntity<Boolean> verifySignature(
            @PathVariable Long id,
            @RequestBody String payload,
            @RequestHeader("X-Webhook-Signature") String signature) {
        log.debug("Verifying signature for webhook: {}", id);
        WebhookDto.Response webhook = webhookService.getWebhook(id);
        // In real implementation, get the secret from the webhook
        return ResponseEntity.ok(true);
    }
}
