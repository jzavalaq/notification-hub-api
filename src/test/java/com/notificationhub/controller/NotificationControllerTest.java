package com.notificationhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.NotificationDto;
import com.notificationhub.dto.PageResponse;
import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("dev")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private NotificationDto.SendRequest sendRequest;
    private NotificationDto.Response response;

    @BeforeEach
    void setUp() {
        sendRequest = NotificationDto.SendRequest.builder()
                .userId("user-123")
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .recipient("user@example.com")
                .subject("Welcome!")
                .content("Welcome to our service!")
                .priority(NotificationPreference.Priority.NORMAL)
                .build();

        response = NotificationDto.Response.builder()
                .id(1L)
                .userId("user-123")
                .notificationType("welcome")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .recipient("user@example.com")
                .subject("Welcome!")
                .content("Welcome to our service!")
                .status(Notification.NotificationStatus.QUEUED)
                .retryCount(0)
                .priority(NotificationPreference.Priority.NORMAL)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser
    void sendNotification_shouldReturn201() throws Exception {
        when(notificationService.sendNotification(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @WithMockUser
    void getNotification_shouldReturn200() throws Exception {
        when(notificationService.getNotification(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void getUserNotifications_shouldReturn200() throws Exception {
        PageResponse<NotificationDto.Response> pageResponse = PageResponse.<NotificationDto.Response>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(notificationService.getUserNotifications("user-123", 0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/notifications/user/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void markAsDelivered_shouldReturn200() throws Exception {
        NotificationDto.Response deliveredResponse = NotificationDto.Response.builder()
                .id(1L)
                .status(Notification.NotificationStatus.DELIVERED)
                .deliveredAt(Instant.now())
                .build();

        when(notificationService.markAsDelivered(1L)).thenReturn(deliveredResponse);

        mockMvc.perform(post("/api/v1/notifications/1/deliver")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser
    void markAsRead_shouldReturn200() throws Exception {
        NotificationDto.Response readResponse = NotificationDto.Response.builder()
                .id(1L)
                .status(Notification.NotificationStatus.READ)
                .readAt(Instant.now())
                .build();

        when(notificationService.markAsRead(1L)).thenReturn(readResponse);

        mockMvc.perform(post("/api/v1/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }
}
