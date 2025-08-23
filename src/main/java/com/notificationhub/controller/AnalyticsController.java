package com.notificationhub.controller;

import com.notificationhub.dto.AnalyticsDto;
import com.notificationhub.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Notification analytics and reporting")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/metrics")
    @Operation(summary = "Get notification metrics")
    public ResponseEntity<AnalyticsDto.Metrics> getMetrics(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        return ResponseEntity.ok(analyticsService.getMetrics(startDate, endDate));
    }

    @GetMapping("/channels")
    @Operation(summary = "Get channel statistics")
    public ResponseEntity<List<AnalyticsDto.ChannelStats>> getChannelStats(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        return ResponseEntity.ok(analyticsService.getChannelStats(startDate, endDate));
    }

    @GetMapping("/engagement/{userId}")
    @Operation(summary = "Get user engagement statistics")
    public ResponseEntity<AnalyticsDto.EngagementStats> getUserEngagement(
            @PathVariable String userId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        return ResponseEntity.ok(analyticsService.getUserEngagement(userId, startDate, endDate));
    }
}
