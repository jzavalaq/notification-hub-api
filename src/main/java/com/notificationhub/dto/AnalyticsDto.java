package com.notificationhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

public class AnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private long totalSent;
        private long totalDelivered;
        private long totalFailed;
        private long totalPending;
        private double deliveryRate;
        private double failureRate;
        private Map<String, Long> byChannel;
        private Map<String, Long> byStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelStats {
        private String channel;
        private long sent;
        private long delivered;
        private long failed;
        private double deliveryRate;
        private double avgDeliveryTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeRequest {
        private Instant startDate;
        private Instant endDate;
        private String userId;
        private String channel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementStats {
        private String userId;
        private long totalReceived;
        private long totalRead;
        private double readRate;
        private Map<String, Long> readsByChannel;
    }
}
