package com.notification.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.hub.dto.AnalyticsDto;
import com.notification.hub.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    private AnalyticsDto.Metrics metrics;
    private AnalyticsDto.ChannelStats channelStats;
    private AnalyticsDto.EngagementStats engagementStats;

    @BeforeEach
    void setUp() {
        Map<String, Long> byChannel = new HashMap<>();
        byChannel.put("EMAIL", 100L);
        byChannel.put("SMS", 50L);

        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("SENT", 100L);
        byStatus.put("DELIVERED", 90L);

        metrics = AnalyticsDto.Metrics.builder()
                .totalSent(100L)
                .totalDelivered(90L)
                .totalFailed(10L)
                .totalPending(5L)
                .deliveryRate(90.0)
                .failureRate(10.0)
                .byChannel(byChannel)
                .byStatus(byStatus)
                .build();

        channelStats = AnalyticsDto.ChannelStats.builder()
                .channel("EMAIL")
                .sent(100L)
                .delivered(90L)
                .failed(10L)
                .deliveryRate(90.0)
                .avgDeliveryTimeMs(150.0)
                .build();

        Map<String, Long> readsByChannel = new HashMap<>();
        readsByChannel.put("EMAIL", 50L);

        engagementStats = AnalyticsDto.EngagementStats.builder()
                .userId("user-123")
                .totalReceived(100L)
                .totalRead(50L)
                .readRate(50.0)
                .readsByChannel(readsByChannel)
                .build();
    }

    @Test
    @WithMockUser
    void getMetrics_shouldReturn200() throws Exception {
        when(analyticsService.getMetrics(any(), any())).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(100))
                .andExpect(jsonPath("$.totalDelivered").value(90))
                .andExpect(jsonPath("$.deliveryRate").value(90.0));
    }

    @Test
    @WithMockUser
    void getMetrics_withDateRange_shouldReturn200() throws Exception {
        Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

        when(analyticsService.getMetrics(any(), any())).thenReturn(metrics);

        mockMvc.perform(get("/api/v1/analytics/metrics")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(100));
    }

    @Test
    @WithMockUser
    void getChannelStats_shouldReturn200() throws Exception {
        when(analyticsService.getChannelStats(any(), any())).thenReturn(List.of(channelStats));

        mockMvc.perform(get("/api/v1/analytics/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[0].sent").value(100));
    }

    @Test
    @WithMockUser
    void getChannelStats_withDateRange_shouldReturn200() throws Exception {
        Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

        when(analyticsService.getChannelStats(any(), any())).thenReturn(List.of(channelStats));

        mockMvc.perform(get("/api/v1/analytics/channels")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channel").value("EMAIL"));
    }

    @Test
    @WithMockUser
    void getUserEngagement_shouldReturn200() throws Exception {
        when(analyticsService.getUserEngagement(eq("user-123"), any(), any())).thenReturn(engagementStats);

        mockMvc.perform(get("/api/v1/analytics/engagement/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.totalReceived").value(100))
                .andExpect(jsonPath("$.totalRead").value(50));
    }

    @Test
    @WithMockUser
    void getUserEngagement_withDateRange_shouldReturn200() throws Exception {
        Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-01-31T23:59:59Z");

        when(analyticsService.getUserEngagement(eq("user-123"), any(), any())).thenReturn(engagementStats);

        mockMvc.perform(get("/api/v1/analytics/engagement/user-123")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }
}
