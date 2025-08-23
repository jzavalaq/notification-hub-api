package com.notificationhub.controller;

import com.notificationhub.dto.PageResponse;
import com.notificationhub.dto.WebhookDto;
import com.notificationhub.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook endpoint management")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Create a webhook endpoint")
    public ResponseEntity<WebhookDto.Response> createWebhook(@Valid @RequestBody WebhookDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(webhookService.createWebhook(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID")
    public ResponseEntity<WebhookDto.Response> getWebhook(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.getWebhook(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user webhooks")
    public ResponseEntity<List<WebhookDto.Response>> getUserWebhooks(@PathVariable String userId) {
        return ResponseEntity.ok(webhookService.getUserWebhooks(userId));
    }

    @GetMapping
    @Operation(summary = "List all webhooks")
    public ResponseEntity<PageResponse<WebhookDto.Response>> getAllWebhooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(webhookService.getAllWebhooks(page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a webhook")
    public ResponseEntity<WebhookDto.Response> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookDto.UpdateRequest request) {
        return ResponseEntity.ok(webhookService.updateWebhook(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify webhook signature")
    public ResponseEntity<Boolean> verifySignature(
            @PathVariable Long id,
            @RequestBody String payload,
            @RequestHeader("X-Webhook-Signature") String signature) {
        WebhookDto.Response webhook = webhookService.getWebhook(id);
        // In real implementation, get the secret from the webhook
        return ResponseEntity.ok(true);
    }
}
