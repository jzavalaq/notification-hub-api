package com.notificationhub.service;

import com.notificationhub.dto.AnalyticsDto;
import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.repository.NotificationRepository;
import com.notificationhub.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for notification analytics and reporting.
 * <p>
 * Provides metrics calculation, channel statistics, and user engagement
 * analytics for notification performance monitoring.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public AnalyticsDto.Metrics getMetrics(Instant startDate, Instant endDate) {
        if (startDate == null) startDate = Instant.now().minus(AppConstants.DEFAULT_ANALYTICS_DAYS, ChronoUnit.DAYS);
        if (endDate == null) endDate = Instant.now();

        long totalSent = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.SENT, startDate, endDate);
        long totalDelivered = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.DELIVERED, startDate, endDate);
        long totalFailed = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.FAILED, startDate, endDate);
        long totalPending = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.PENDING, startDate, endDate)
                + notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.QUEUED, startDate, endDate);

        long totalProcessed = totalSent + totalDelivered + totalFailed;
        double deliveryRate = totalProcessed > 0 ? (double) totalDelivered / totalProcessed * 100 : 0;
        double failureRate = totalProcessed > 0 ? (double) totalFailed / totalProcessed * 100 : 0;

        Map<String, Long> byChannel = new HashMap<>();
        List<Object[]> channelCounts = notificationRepository.countGroupByChannel(startDate, endDate);
        for (Object[] row : channelCounts) {
            byChannel.put(((NotificationTemplate.ChannelType) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> byStatus = new HashMap<>();
        List<Object[]> statusCounts = notificationRepository.countGroupByStatus(startDate, endDate);
        for (Object[] row : statusCounts) {
            byStatus.put(((Notification.NotificationStatus) row[0]).name(), (Long) row[1]);
        }

        return AnalyticsDto.Metrics.builder()
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalFailed(totalFailed)
                .totalPending(totalPending)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .failureRate(Math.round(failureRate * 100.0) / 100.0)
                .byChannel(byChannel)
                .byStatus(byStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDto.ChannelStats> getChannelStats(Instant startDate, Instant endDate) {
        if (startDate == null) startDate = Instant.now().minus(AppConstants.DEFAULT_ANALYTICS_DAYS, ChronoUnit.DAYS);
        if (endDate == null) endDate = Instant.now();

        List<AnalyticsDto.ChannelStats> stats = new java.util.ArrayList<>();

        for (NotificationTemplate.ChannelType channel : NotificationTemplate.ChannelType.values()) {
            long sent = notificationRepository.countByChannelAndDateRange(channel, startDate, endDate);
            long delivered = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.DELIVERED, startDate, endDate);
            long failed = notificationRepository.countByStatusAndDateRange(Notification.NotificationStatus.FAILED, startDate, endDate);

            double deliveryRate = sent > 0 ? (double) delivered / sent * 100 : 0;

            stats.add(AnalyticsDto.ChannelStats.builder()
                    .channel(channel.name())
                    .sent(sent)
                    .delivered(delivered)
                    .failed(failed)
                    .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                    .avgDeliveryTimeMs(0.0) // Would calculate from actual delivery times
                    .build());
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.EngagementStats getUserEngagement(String userId, Instant startDate, Instant endDate) {
        if (startDate == null) startDate = Instant.now().minus(AppConstants.DEFAULT_ANALYTICS_DAYS, ChronoUnit.DAYS);
        if (endDate == null) endDate = Instant.now();

        List<Notification> notifications = notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.DELIVERED);
        notifications.addAll(notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.READ));

        long totalReceived = notifications.size();
        long totalRead = notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.READ).size();
        double readRate = totalReceived > 0 ? (double) totalRead / totalReceived * 100 : 0;

        Map<String, Long> readsByChannel = new HashMap<>();
        for (Notification n : notifications) {
            if (n.getStatus() == Notification.NotificationStatus.READ) {
                String channel = n.getChannel().name();
                readsByChannel.merge(channel, 1L, Long::sum);
            }
        }

        return AnalyticsDto.EngagementStats.builder()
                .userId(userId)
                .totalReceived(totalReceived)
                .totalRead(totalRead)
                .readRate(Math.round(readRate * 100.0) / 100.0)
                .readsByChannel(readsByChannel)
                .build();
    }
}
