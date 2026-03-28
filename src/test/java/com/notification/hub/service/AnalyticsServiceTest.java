package com.notification.hub.service;

import com.notification.hub.dto.AnalyticsDto;
import com.notification.hub.entity.Notification;
import com.notification.hub.entity.NotificationTemplate;
import com.notification.hub.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AnalyticsServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Instant startDate;
    private Instant endDate;

    @BeforeEach
    void setUp() {
        startDate = Instant.now().minus(7, ChronoUnit.DAYS);
        endDate = Instant.now();
    }

    @Test
    void getMetrics_validDateRange_returnsMetrics() {
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.SENT), any(), any()))
                .thenReturn(100L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.DELIVERED), any(), any()))
                .thenReturn(90L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.FAILED), any(), any()))
                .thenReturn(10L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.PENDING), any(), any()))
                .thenReturn(5L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.QUEUED), any(), any()))
                .thenReturn(3L);
        when(notificationRepository.countGroupByChannel(any(), any())).thenReturn(List.of());
        when(notificationRepository.countGroupByStatus(any(), any())).thenReturn(List.of());

        AnalyticsDto.Metrics metrics = analyticsService.getMetrics(startDate, endDate);

        assertNotNull(metrics);
        assertEquals(100L, metrics.getTotalSent());
        assertEquals(90L, metrics.getTotalDelivered());
        assertEquals(10L, metrics.getTotalFailed());
        assertEquals(8L, metrics.getTotalPending());
    }

    @Test
    void getMetrics_nullDates_usesDefaults() {
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countGroupByChannel(any(), any())).thenReturn(List.of());
        when(notificationRepository.countGroupByStatus(any(), any())).thenReturn(List.of());

        AnalyticsDto.Metrics metrics = analyticsService.getMetrics(null, null);

        assertNotNull(metrics);
    }

    @Test
    void getMetrics_zeroProcessed_handlesGracefully() {
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countGroupByChannel(any(), any())).thenReturn(List.of());
        when(notificationRepository.countGroupByStatus(any(), any())).thenReturn(List.of());

        AnalyticsDto.Metrics metrics = analyticsService.getMetrics(startDate, endDate);

        assertNotNull(metrics);
        assertEquals(0.0, metrics.getDeliveryRate());
        assertEquals(0.0, metrics.getFailureRate());
    }

    @Test
    void getMetrics_withChannelCounts_populatesByChannel() {
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countGroupByChannel(any(), any())).thenReturn(List.of(
                new Object[]{NotificationTemplate.ChannelType.EMAIL, 50L},
                new Object[]{NotificationTemplate.ChannelType.SMS, 30L}
        ));
        when(notificationRepository.countGroupByStatus(any(), any())).thenReturn(List.of());

        AnalyticsDto.Metrics metrics = analyticsService.getMetrics(startDate, endDate);

        assertNotNull(metrics);
        assertNotNull(metrics.getByChannel());
        assertTrue(metrics.getByChannel().containsKey("EMAIL"));
        assertTrue(metrics.getByChannel().containsKey("SMS"));
    }

    @Test
    void getMetrics_withStatusCounts_populatesByStatus() {
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countGroupByChannel(any(), any())).thenReturn(List.of());
        when(notificationRepository.countGroupByStatus(any(), any())).thenReturn(List.of(
                new Object[]{Notification.NotificationStatus.SENT, 100L},
                new Object[]{Notification.NotificationStatus.DELIVERED, 90L}
        ));

        AnalyticsDto.Metrics metrics = analyticsService.getMetrics(startDate, endDate);

        assertNotNull(metrics);
        assertNotNull(metrics.getByStatus());
        assertTrue(metrics.getByStatus().containsKey("SENT"));
        assertTrue(metrics.getByStatus().containsKey("DELIVERED"));
    }

    @Test
    void getChannelStats_validDateRange_returnsStats() {
        when(notificationRepository.countByChannelAndDateRange(any(), any(), any())).thenReturn(100L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.DELIVERED), any(), any()))
                .thenReturn(90L);
        when(notificationRepository.countByStatusAndDateRange(eq(Notification.NotificationStatus.FAILED), any(), any()))
                .thenReturn(10L);

        List<AnalyticsDto.ChannelStats> stats = analyticsService.getChannelStats(startDate, endDate);

        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        assertEquals(4, stats.size()); // EMAIL, SMS, PUSH, IN_APP
    }

    @Test
    void getChannelStats_nullDates_usesDefaults() {
        when(notificationRepository.countByChannelAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);

        List<AnalyticsDto.ChannelStats> stats = analyticsService.getChannelStats(null, null);

        assertNotNull(stats);
        assertFalse(stats.isEmpty());
    }

    @Test
    void getChannelStats_zeroSent_handlesGracefully() {
        when(notificationRepository.countByChannelAndDateRange(any(), any(), any())).thenReturn(0L);
        when(notificationRepository.countByStatusAndDateRange(any(), any(), any())).thenReturn(0L);

        List<AnalyticsDto.ChannelStats> stats = analyticsService.getChannelStats(startDate, endDate);

        assertNotNull(stats);
        for (AnalyticsDto.ChannelStats stat : stats) {
            assertEquals(0.0, stat.getDeliveryRate());
        }
    }

    @Test
    void getUserEngagement_validUser_returnsStats() {
        Notification delivered = Notification.builder()
                .id(1L)
                .userId("user-123")
                .status(Notification.NotificationStatus.DELIVERED)
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .build();
        Notification read = Notification.builder()
                .id(2L)
                .userId("user-123")
                .status(Notification.NotificationStatus.READ)
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .build();

        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.DELIVERED))
                .thenReturn(new java.util.ArrayList<>(List.of(delivered)));
        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.READ))
                .thenReturn(new java.util.ArrayList<>(List.of(read)));

        AnalyticsDto.EngagementStats stats = analyticsService.getUserEngagement("user-123", startDate, endDate);

        assertNotNull(stats);
        assertEquals("user-123", stats.getUserId());
        assertEquals(2L, stats.getTotalReceived());
        assertEquals(1L, stats.getTotalRead());
    }

    @Test
    void getUserEngagement_nullDates_usesDefaults() {
        when(notificationRepository.findByUserIdAndStatus(any(), any())).thenReturn(new java.util.ArrayList<>());

        AnalyticsDto.EngagementStats stats = analyticsService.getUserEngagement("user-123", null, null);

        assertNotNull(stats);
        assertEquals("user-123", stats.getUserId());
    }

    @Test
    void getUserEngagement_noNotifications_handlesGracefully() {
        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.DELIVERED))
                .thenReturn(Collections.emptyList());
        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.READ))
                .thenReturn(Collections.emptyList());

        AnalyticsDto.EngagementStats stats = analyticsService.getUserEngagement("user-123", startDate, endDate);

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalReceived());
        assertEquals(0L, stats.getTotalRead());
        assertEquals(0.0, stats.getReadRate());
    }

    @Test
    void getUserEngagement_withReadNotifications_populatesByChannel() {
        Notification readEmail = Notification.builder()
                .id(1L)
                .userId("user-123")
                .status(Notification.NotificationStatus.READ)
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .build();
        Notification readSms = Notification.builder()
                .id(2L)
                .userId("user-123")
                .status(Notification.NotificationStatus.READ)
                .channel(NotificationTemplate.ChannelType.SMS)
                .build();

        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.DELIVERED))
                .thenReturn(new java.util.ArrayList<>(List.of(readEmail, readSms)));
        when(notificationRepository.findByUserIdAndStatus("user-123", Notification.NotificationStatus.READ))
                .thenReturn(new java.util.ArrayList<>(List.of(readEmail, readSms)));

        AnalyticsDto.EngagementStats stats = analyticsService.getUserEngagement("user-123", startDate, endDate);

        assertNotNull(stats);
        assertNotNull(stats.getReadsByChannel());
    }
}
