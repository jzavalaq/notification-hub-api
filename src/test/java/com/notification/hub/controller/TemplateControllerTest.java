package com.notification.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.hub.dto.TemplateDto;
import com.notification.hub.entity.NotificationTemplate;
import com.notification.hub.service.TemplateService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
@ActiveProfiles("test")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateService templateService;

    private TemplateDto.CreateRequest createRequest;
    private TemplateDto.Response response;

    @BeforeEach
    void setUp() {
        createRequest = TemplateDto.CreateRequest.builder()
                .code("welcome-email")
                .name("Welcome Email")
                .subject("Welcome {{name}}!")
                .body("Hello {{name}}, welcome to our service!")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .build();

        response = TemplateDto.Response.builder()
                .id(1L)
                .code("welcome-email")
                .name("Welcome Email")
                .subject("Welcome {{name}}!")
                .body("Hello {{name}}, welcome to our service!")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser
    void createTemplate_shouldReturn201() throws Exception {
        when(templateService.createTemplate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("welcome-email"))
                .andExpect(jsonPath("$.name").value("Welcome Email"));
    }

    @Test
    @WithMockUser
    void getTemplate_shouldReturn200() throws Exception {
        when(templateService.getTemplate(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("welcome-email"));
    }

    @Test
    @WithMockUser
    void getTemplateByCode_shouldReturn200() throws Exception {
        when(templateService.getTemplateByCode("welcome-email")).thenReturn(response);

        mockMvc.perform(get("/api/v1/templates/code/welcome-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("welcome-email"));
    }

    @Test
    @WithMockUser
    void updateTemplate_shouldReturn200() throws Exception {
        TemplateDto.UpdateRequest updateRequest = TemplateDto.UpdateRequest.builder()
                .name("Updated Welcome Email")
                .build();

        TemplateDto.Response updatedResponse = TemplateDto.Response.builder()
                .id(1L)
                .code("welcome-email")
                .name("Updated Welcome Email")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .build();

        when(templateService.updateTemplate(eq(1L), any())).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/templates/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Welcome Email"));
    }

    @Test
    @WithMockUser
    void deleteTemplate_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/templates/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
