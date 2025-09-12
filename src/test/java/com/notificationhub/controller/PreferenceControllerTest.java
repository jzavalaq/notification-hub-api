package com.notificationhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.PreferenceDto;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.service.PreferenceService;
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
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PreferenceController.class)
@ActiveProfiles("test")
class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreferenceService preferenceService;

    private PreferenceDto.CreateRequest createRequest;
    private PreferenceDto.Response response;

    @BeforeEach
    void setUp() {
        Set<NotificationTemplate.ChannelType> channels = new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL));

        createRequest = PreferenceDto.CreateRequest.builder()
                .userId("user-123")
                .enabledChannels(channels)
                .optedOutTypes(new HashSet<>())
                .timezone("UTC")
                .defaultPriority(NotificationPreference.Priority.NORMAL)
                .build();

        response = PreferenceDto.Response.builder()
                .id(1L)
                .userId("user-123")
                .enabledChannels(channels)
                .optedOutTypes(new HashSet<>())
                .timezone("UTC")
                .defaultPriority(NotificationPreference.Priority.NORMAL)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser
    void createPreference_shouldReturn201() throws Exception {
        when(preferenceService.createPreference(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void getPreference_shouldReturn200() throws Exception {
        when(preferenceService.getPreference("user-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/preferences/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void getPreferenceOrDefault_shouldReturn200() throws Exception {
        when(preferenceService.getPreferenceOrDefault("user-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/preferences/user-123/or-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void updatePreference_shouldReturn200() throws Exception {
        PreferenceDto.UpdateRequest updateRequest = PreferenceDto.UpdateRequest.builder()
                .timezone("America/New_York")
                .build();

        when(preferenceService.updatePreference(eq("user-123"), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/preferences/user-123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    @WithMockUser
    void deletePreference_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/preferences/user-123")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
