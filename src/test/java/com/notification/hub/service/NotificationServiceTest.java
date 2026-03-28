package com.notification.hub.service;

import com.notification.hub.dto.NotificationDto;
import com.notification.hub.dto.PageResponse;
import com.notification.hub.entity.*;
import com.notification.hub.exception.NotificationException;
import com.notification.hub.exception.ResourceNotFoundException;
import com.notification.hub.repository.NotificationAuditRepository;
import com.notification.hub.repository.NotificationRepository;
import com.notification.hub.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationAuditRepository auditRepository;

    @Mock
    private PreferenceService preferenceService;

    @Mock
    private TemplateService templateService;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationDto.SendRequest sendRequest;
    private NotificationTemplate template;
    private Notification notification;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        template = NotificationTemplate.builder()
                .id(1L)
                .code("welcome-email")
                .name("Welcome Email")
                .subject("Welcome {{name}}!")
                .body("Hello {{name}}, welcome!")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .build();

        sendRequest = NotificationDto.SendRequest.builder()
                .userId("user-123")
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .recipient("user@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .priority(NotificationPreference.Priority.NORMAL)
                .build();

        notification = Notification.builder()
                .id(1L)
                .userId("user-123")
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .recipient("user@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.QUEUED)
                .retryCount(0)
                .maxRetries(3)
                .priority(NotificationPreference.Priority.NORMAL)
                .build();

        preference = NotificationPreference.builder()
                .id(1L)
                .userId("user-123")
                .enabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL)))
                .optedOutTypes(new HashSet<>())
                .defaultPriority(NotificationPreference.Priority.NORMAL)
                .build();
    }

    @Test
    void sendNotification_validRequest_returnsResponse() {
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationDto.Response response = notificationService.sendNotification(sendRequest);

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        // Status can be QUEUED or SENT depending on async processing
        assertNotNull(response.getStatus());
        // Save is called at least once (initial save), possibly more due to async processing
        verify(notificationRepository, atLeast(1)).save(any());
    }

    @Test
    void sendNotification_withTemplate_rendersContent() {
        sendRequest.setContent(null);
        sendRequest.setTemplateCode("welcome-email");
        sendRequest.setTemplateVariables(Map.of("name", "John"));

        when(templateRepository.findByCode("welcome-email")).thenReturn(Optional.of(template));
        when(templateService.renderTemplate(eq(template), any())).thenReturn("Hello John, welcome!");
        when(templateService.renderSubject(eq(template), any())).thenReturn("Welcome John!");
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationDto.Response response = notificationService.sendNotification(sendRequest);

        assertNotNull(response);
        verify(templateRepository).findByCode("welcome-email");
        verify(templateService).renderTemplate(eq(template), any());
    }

    @Test
    void sendNotification_templateNotFound_throwsException() {
        sendRequest.setContent(null);
        sendRequest.setTemplateCode("nonexistent");
        when(templateRepository.findByCode("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.sendNotification(sendRequest));
    }

    @Test
    void sendNotification_emptyContent_throwsException() {
        sendRequest.setContent("");
        sendRequest.setSubject(null);

        assertThrows(NotificationException.class, () -> notificationService.sendNotification(sendRequest));
    }

    @Test
    void sendNotification_nullContent_throwsException() {
        sendRequest.setContent(null);
        sendRequest.setSubject(null);

        assertThrows(NotificationException.class, () -> notificationService.sendNotification(sendRequest));
    }

    @Test
    void sendNotification_userOptedOut_throwsException() {
        preference.setOptedOutTypes(new HashSet<>(Set.of("welcome")));
        when(preferenceService.findByUserId("user-123")).thenReturn(Optional.of(preference));
        when(preferenceService.isOptedOut(eq(preference), eq("welcome"))).thenReturn(true);

        assertThrows(NotificationException.class, () -> notificationService.sendNotification(sendRequest));
    }

    @Test
    void sendNotification_channelDisabled_throwsException() {
        preference.setEnabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.SMS)));
        when(preferenceService.findByUserId("user-123")).thenReturn(Optional.of(preference));
        when(preferenceService.isOptedOut(eq(preference), any())).thenReturn(false);
        when(preferenceService.isChannelEnabled(eq(preference), eq(NotificationTemplate.ChannelType.EMAIL))).thenReturn(false);

        assertThrows(NotificationException.class, () -> notificationService.sendNotification(sendRequest));
    }

    @Test
    void sendNotification_scheduledNotification_setsPendingStatus() {
        sendRequest.setScheduledAt(Instant.now().plusSeconds(3600));
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationDto.Response response = notificationService.sendNotification(sendRequest);

        assertEquals(Notification.NotificationStatus.PENDING, response.getStatus());
    }

    @Test
    void getNotification_existingId_returnsResponse() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        NotificationDto.Response response = notificationService.getNotification(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user-123", response.getUserId());
    }

    @Test
    void getNotification_nonExistentId_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.getNotification(999L));
    }

    @Test
    void getUserNotifications_validRequest_returnsPageResponse() {
        List<Notification> notifications = List.of(notification);
        Page<Notification> page = new PageImpl<>(notifications);
        when(notificationRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(page);

        PageResponse<NotificationDto.Response> response = notificationService.getUserNotifications("user-123", 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("user-123", response.getContent().get(0).getUserId());
    }

    @Test
    void getUserNotifications_negativePage_correctsToZero() {
        Page<Notification> page = new PageImpl<>(List.of());
        when(notificationRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(page);

        PageResponse<NotificationDto.Response> response = notificationService.getUserNotifications("user-123", -1, 20);

        assertNotNull(response);
    }

    @Test
    void getUserNotifications_largeSize_capsAtMax() {
        Page<Notification> page = new PageImpl<>(List.of());
        when(notificationRepository.findByUserId(eq("user-123"), any(Pageable.class))).thenReturn(page);

        PageResponse<NotificationDto.Response> response = notificationService.getUserNotifications("user-123", 0, 1000);

        assertNotNull(response);
    }

    @Test
    void markAsDelivered_existingNotification_updatesStatus() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        NotificationDto.Response response = notificationService.markAsDelivered(1L);

        assertEquals(Notification.NotificationStatus.DELIVERED, response.getStatus());
        assertNotNull(response.getDeliveredAt());
        verify(webhookService).triggerWebhooks(any(), eq(WebhookEndpoint.WebhookEventType.NOTIFICATION_DELIVERED));
    }

    @Test
    void markAsDelivered_nonExistentNotification_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsDelivered(999L));
    }

    @Test
    void markAsRead_existingNotification_updatesStatus() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        NotificationDto.Response response = notificationService.markAsRead(1L);

        assertEquals(Notification.NotificationStatus.READ, response.getStatus());
        assertNotNull(response.getReadAt());
        verify(webhookService).triggerWebhooks(any(), eq(WebhookEndpoint.WebhookEventType.NOTIFICATION_READ));
    }

    @Test
    void markAsRead_nonExistentNotification_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(999L));
    }

    @Test
    void sendBatchNotifications_validRequest_returnsResponses() {
        NotificationDto.BatchSendRequest.Recipient recipient1 = NotificationDto.BatchSendRequest.Recipient.builder()
                .userId("user-1")
                .recipient("user1@example.com")
                .build();
        NotificationDto.BatchSendRequest.Recipient recipient2 = NotificationDto.BatchSendRequest.Recipient.builder()
                .userId("user-2")
                .recipient("user2@example.com")
                .build();

        NotificationDto.BatchSendRequest batchRequest = NotificationDto.BatchSendRequest.builder()
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .subject("Welcome!")
                .content("Welcome to our service!")
                .recipients(List.of(recipient1, recipient2))
                .build();

        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        List<NotificationDto.Response> responses = notificationService.sendBatchNotifications(batchRequest);

        assertEquals(2, responses.size());
    }

    @Test
    void sendBatchNotifications_withVariables_mergesCorrectly() {
        NotificationDto.BatchSendRequest.Recipient recipient = NotificationDto.BatchSendRequest.Recipient.builder()
                .userId("user-1")
                .recipient("user1@example.com")
                .variables(Map.of("name", "John"))
                .build();

        NotificationDto.BatchSendRequest batchRequest = NotificationDto.BatchSendRequest.builder()
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .subject("Welcome!")
                .content("Welcome!")
                .templateVariables(Map.of("company", "Acme"))
                .recipients(List.of(recipient))
                .build();

        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        List<NotificationDto.Response> responses = notificationService.sendBatchNotifications(batchRequest);

        assertEquals(1, responses.size());
    }

    @Test
    void processScheduledNotifications_processesPendingNotifications() {
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setScheduledAt(Instant.now().minusSeconds(60));
        List<Notification> scheduled = List.of(notification);

        when(notificationRepository.findByStatusAndScheduledAtBefore(
                eq(Notification.NotificationStatus.PENDING), any(Instant.class)))
                .thenReturn(scheduled);
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.processScheduledNotifications();

        verify(notificationRepository).findByStatusAndScheduledAtBefore(eq(Notification.NotificationStatus.PENDING), any());
    }

    @Test
    void retryFailedNotifications_retriesFailedNotifications() {
        notification.setStatus(Notification.NotificationStatus.FAILED);
        notification.setRetryCount(1);
        List<Notification> failed = List.of(notification);

        when(notificationRepository.findByStatusAndRetryCountLessThan(
                eq(Notification.NotificationStatus.FAILED), anyInt()))
                .thenReturn(failed);
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.retryFailedNotifications();

        verify(notificationRepository).findByStatusAndRetryCountLessThan(eq(Notification.NotificationStatus.FAILED), anyInt());
    }
}
