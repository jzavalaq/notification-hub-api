package com.notification.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.hub.dto.PageResponse;
import com.notification.hub.dto.WebhookDto;
import com.notification.hub.entity.WebhookEndpoint;
import com.notification.hub.service.WebhookService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
@ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookService webhookService;

    private WebhookDto.CreateRequest createRequest;
    private WebhookDto.Response response;

    @BeforeEach
    void setUp() {
        Set<WebhookEndpoint.WebhookEventType> events = new HashSet<>(Set.of(WebhookEndpoint.WebhookEventType.NOTIFICATION_SENT));

        createRequest = WebhookDto.CreateRequest.builder()
                .userId("user-123")
                .name("Test Webhook")
                .url("https://example.com/webhook")
                .subscribedEvents(events)
                .build();

        response = WebhookDto.Response.builder()
                .id(1L)
                .userId("user-123")
                .name("Test Webhook")
                .url("https://example.com/webhook")
                .subscribedEvents(events)
                .status(WebhookEndpoint.WebhookStatus.ACTIVE)
                .maxRetries(3)
                .timeoutSeconds(30)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser
    void createWebhook_shouldReturn201() throws Exception {
        when(webhookService.createWebhook(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/webhooks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Webhook"));
    }

    @Test
    @WithMockUser
    void getWebhook_shouldReturn200() throws Exception {
        when(webhookService.getWebhook(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/webhooks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void getUserWebhooks_shouldReturn200() throws Exception {
        PageResponse<WebhookDto.Response> pageResponse = PageResponse.<WebhookDto.Response>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        when(webhookService.getUserWebhooks("user-123", 0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/webhooks/user/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void getAllWebhooks_shouldReturn200() throws Exception {
        PageResponse<WebhookDto.Response> pageResponse = PageResponse.<WebhookDto.Response>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(webhookService.getAllWebhooks(0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Webhook"));
    }

    @Test
    @WithMockUser
    void updateWebhook_shouldReturn200() throws Exception {
        WebhookDto.UpdateRequest updateRequest = WebhookDto.UpdateRequest.builder()
                .name("Updated Webhook")
                .build();

        when(webhookService.updateWebhook(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/webhooks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteWebhook_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/webhooks/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void verifySignature_shouldReturn200() throws Exception {
        when(webhookService.getWebhook(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/webhooks/1/verify")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("test-payload")
                        .header("X-Webhook-Signature", "test-signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
