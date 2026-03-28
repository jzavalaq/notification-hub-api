package com.notification.hub.controller;

import com.notification.hub.dto.AnalyticsDto;
import com.notification.hub.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for notification analytics and reporting.
 * <p>
 * Provides metrics, channel statistics, and user engagement data
 * for analyzing notification performance.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Notification analytics and reporting")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Retrieves overall notification metrics.
     *
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return notification metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get notification metrics")
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    public ResponseEntity<AnalyticsDto.Metrics> getMetrics(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        log.debug("Fetching metrics from {} to {}", startDate, endDate);
        return ResponseEntity.ok(analyticsService.getMetrics(startDate, endDate));
    }

    /**
     * Retrieves statistics broken down by notification channel.
     *
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return list of channel statistics
     */
    @GetMapping("/channels")
    @Operation(summary = "Get channel statistics")
    @ApiResponse(responseCode = "200", description = "Channel statistics retrieved successfully")
    public ResponseEntity<List<AnalyticsDto.ChannelStats>> getChannelStats(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        log.debug("Fetching channel stats from {} to {}", startDate, endDate);
        return ResponseEntity.ok(analyticsService.getChannelStats(startDate, endDate));
    }

    /**
     * Retrieves engagement statistics for a specific user.
     *
     * @param userId the user ID
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return user engagement statistics
     */
    @GetMapping("/engagement/{userId}")
    @Operation(summary = "Get user engagement statistics")
    @ApiResponse(responseCode = "200", description = "Engagement statistics retrieved successfully")
    public ResponseEntity<AnalyticsDto.EngagementStats> getUserEngagement(
            @PathVariable String userId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        log.debug("Fetching engagement stats for user: {} from {} to {}", userId, startDate, endDate);
        return ResponseEntity.ok(analyticsService.getUserEngagement(userId, startDate, endDate));
    }
}
