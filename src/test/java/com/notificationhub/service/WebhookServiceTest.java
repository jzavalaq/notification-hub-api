package com.notificationhub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.PageResponse;
import com.notificationhub.dto.WebhookDto;
import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.entity.WebhookDelivery;
import com.notificationhub.entity.WebhookEndpoint;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.WebhookDeliveryRepository;
import com.notificationhub.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebhookServiceTest {

    @Mock
    private WebhookEndpointRepository webhookRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WebhookService webhookService;

    private WebhookEndpoint webhook;
    private WebhookDto.CreateRequest createRequest;
    private WebhookDto.UpdateRequest updateRequest;
    private Notification notification;

    @BeforeEach
    void setUp() {
        webhook = WebhookEndpoint.builder()
                .id(1L)
                .userId("user-123")
                .name("Test Webhook")
                .url("https://example.com/webhook")
                .secret("test-secret")
                .subscribedEvents(new HashSet<>(Set.of(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT)))
                .status(WebhookEndpoint.WebhookStatus.ACTIVE)
                .maxRetries(3)
                .timeoutSeconds(30)
                .build();

        createRequest = WebhookDto.CreateRequest.builder()
                .userId("user-123")
                .name("Test Webhook")
                .url("https://example.com/webhook")
                .subscribedEvents(new HashSet<>(Set.of(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT)))
                .build();

        updateRequest = WebhookDto.UpdateRequest.builder()
                .name("Updated Webhook")
                .build();

        notification = Notification.builder()
                .id(1L)
                .userId("user-123")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .status(Notification.NotificationStatus.SENT)
                .build();
    }

    @Test
    void createWebhook_validRequest_returnsResponse() {
        when(webhookRepository.save(any())).thenAnswer(inv -> {
            WebhookEndpoint w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WebhookDto.Response response = webhookService.createWebhook(createRequest);

        assertNotNull(response);
        assertEquals("Test Webhook", response.getName());
        assertEquals("https://example.com/webhook", response.getUrl());
        verify(webhookRepository).save(any());
    }

    @Test
    void createWebhook_nullSecret_generatesSecret() {
        createRequest.setSecret(null);

        when(webhookRepository.save(any())).thenAnswer(inv -> {
            WebhookEndpoint w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WebhookDto.Response response = webhookService.createWebhook(createRequest);

        assertNotNull(response);
        verify(webhookRepository).save(any());
    }

    @Test
    void createWebhook_nullMaxRetries_setsDefault() {
        createRequest.setMaxRetries(null);
        createRequest.setTimeoutSeconds(null);

        when(webhookRepository.save(any())).thenAnswer(inv -> {
            WebhookEndpoint w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WebhookDto.Response response = webhookService.createWebhook(createRequest);

        assertNotNull(response);
        verify(webhookRepository).save(any());
    }

    @Test
    void updateWebhook_existingId_returnsResponse() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookRepository.save(any())).thenReturn(webhook);

        WebhookDto.Response response = webhookService.updateWebhook(1L, updateRequest);

        assertNotNull(response);
        verify(webhookRepository).save(any());
    }

    @Test
    void updateWebhook_nonExistentId_throwsException() {
        when(webhookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> webhookService.updateWebhook(999L, updateRequest));
    }

    @Test
    void updateWebhook_partialUpdate_onlyUpdatesProvidedFields() {
        WebhookDto.UpdateRequest partialUpdate = WebhookDto.UpdateRequest.builder()
                .status(WebhookEndpoint.WebhookStatus.DISABLED)
                .build();

        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookRepository.save(any())).thenReturn(webhook);

        WebhookDto.Response response = webhookService.updateWebhook(1L, partialUpdate);

        assertNotNull(response);
        verify(webhookRepository).save(any());
    }

    @Test
    void getWebhook_existingId_returnsResponse() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));

        WebhookDto.Response response = webhookService.getWebhook(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getWebhook_nonExistentId_throwsException() {
        when(webhookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> webhookService.getWebhook(999L));
    }

    @Test
    void getUserWebhooks_existingUser_returnsList() {
        when(webhookRepository.findByUserId("user-123")).thenReturn(List.of(webhook));

        List<WebhookDto.Response> responses = webhookService.getUserWebhooks("user-123");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void getUserWebhooks_userWithNoWebhooks_returnsEmptyList() {
        when(webhookRepository.findByUserId("nonexistent")).thenReturn(List.of());

        List<WebhookDto.Response> responses = webhookService.getUserWebhooks("nonexistent");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void getAllWebhooks_validRequest_returnsPageResponse() {
        Page<WebhookEndpoint> page = new PageImpl<>(List.of(webhook));
        when(webhookRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<WebhookDto.Response> response = webhookService.getAllWebhooks(0, 20);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getAllWebhooks_negativePage_correctsToZero() {
        Page<WebhookEndpoint> page = new PageImpl<>(List.of());
        when(webhookRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<WebhookDto.Response> response = webhookService.getAllWebhooks(-1, 20);

        assertNotNull(response);
    }

    @Test
    void getAllWebhooks_largeSize_capsAtMax() {
        Page<WebhookEndpoint> page = new PageImpl<>(List.of());
        when(webhookRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<WebhookDto.Response> response = webhookService.getAllWebhooks(0, 1000);

        assertNotNull(response);
    }

    @Test
    void deleteWebhook_existingId_deletesSuccessfully() {
        when(webhookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(webhookRepository).deleteById(1L);

        assertDoesNotThrow(() -> webhookService.deleteWebhook(1L));
        verify(webhookRepository).deleteById(1L);
    }

    @Test
    void deleteWebhook_nonExistentId_throwsException() {
        when(webhookRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> webhookService.deleteWebhook(999L));
    }

    @Test
    void verifySignature_validSignature_returnsTrue() throws Exception {
        String payload = "{\"test\":\"data\"}";
        String secret = "test-secret";

        // Generate a valid signature
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signature = "sha256=" + java.util.Base64.getEncoder().encodeToString(hash);

        boolean result = webhookService.verifySignature(payload, signature, secret);

        assertTrue(result);
    }

    @Test
    void verifySignature_invalidSignature_returnsFalse() {
        String payload = "{\"test\":\"data\"}";
        String secret = "test-secret";
        String invalidSignature = "sha256=invalid";

        boolean result = webhookService.verifySignature(payload, invalidSignature, secret);

        assertFalse(result);
    }

    @Test
    void triggerWebhooks_activeWebhooks_processesSuccessfully() {
        when(webhookRepository.findBySubscribedEventsContaining(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT))
                .thenReturn(List.of(webhook));

        webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT);

        verify(webhookRepository).findBySubscribedEventsContaining(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT);
    }

    @Test
    void triggerWebhooks_disabledWebhook_skipsWebhook() {
        webhook.setStatus(WebhookEndpoint.WebhookStatus.DISABLED);
        when(webhookRepository.findBySubscribedEventsContaining(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT))
                .thenReturn(List.of(webhook));

        webhookService.triggerWebhooks(notification, WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT);

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void processRetries_pendingDeliveries_retriesDelivery() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(1L)
                .webhook(webhook)
                .notification(notification)
                .eventType(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT)
                .status(WebhookDelivery.DeliveryStatus.RETRYING)
                .nextRetryAt(Instant.now().minusSeconds(60))
                .build();

        when(deliveryRepository.findByStatusAndNextRetryAtBefore(
                eq(WebhookDelivery.DeliveryStatus.RETRYING), any(Instant.class)))
                .thenReturn(List.of(delivery));

        webhookService.processRetries();

        verify(deliveryRepository).findByStatusAndNextRetryAtBefore(
                eq(WebhookDelivery.DeliveryStatus.RETRYING), any(Instant.class));
    }
}
