package com.notification.hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Objects for analytics operations.
 * Provides metrics, channel statistics, and user engagement data.
 */
public class AnalyticsDto {

    /**
     * DTO containing overall notification metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        /** Total number of notifications sent. */
        private long totalSent;
        /** Total number of notifications delivered. */
        private long totalDelivered;
        /** Total number of notifications that failed. */
        private long totalFailed;
        /** Total number of notifications pending. */
        private long totalPending;
        /** Delivery rate as a percentage. */
        private double deliveryRate;
        /** Failure rate as a percentage. */
        private double failureRate;
        /** Breakdown by notification channel. */
        private Map<String, Long> byChannel;
        /** Breakdown by notification status. */
        private Map<String, Long> byStatus;
    }

    /**
     * DTO containing statistics for a single notification channel.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelStats {
        /** The of the channel (EMAIL, SMS, PUSH, IN-app). */
        private String channel;
        /** Number of notifications sent via this channel. */
        private long sent;
        /** Number of notifications delivered via this channel. */
        private long delivered;
        /** Number of notifications failed via this channel. */
        private long failed;
        /** Delivery rate as a percentage. */
        private double deliveryRate;
        /** Average delivery time in milliseconds. */
        private double avgDeliveryTimeMs;
    }

    /**
     * Request DTO for date range queries.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeRequest {
        /** Start date for the date range (optional). */
        private Instant startDate;
        /** end date for the date range (optional). */
        private Instant endDate;
        /** User ID for filter by (optional). */
        private String userId;
        /** Channel type to filter by (optional). */
        private String channel;
    }

    /**
     * DTO containing user engagement statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementStats {
        /** User ID for engagement statistics. */
        private String userId;
        /** Total number of notifications received by the user. */
        private long totalReceived;
        /** Total number of notifications read by the user. */
        private long totalRead;
        /** Read rate as a percentage. */
        private double readRate;
        /** Breakdown of reads by notification channel. */
        private Map<String, Long> readsByChannel;
    }
}
