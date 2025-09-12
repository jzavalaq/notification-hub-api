package com.notificationhub.service;

import com.notificationhub.dto.TemplateDto;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.exception.DuplicateResourceException;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private TemplateService templateService;

    private NotificationTemplate template;
    private TemplateDto.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        template = NotificationTemplate.builder()
                .id(1L)
                .code("welcome-email")
                .name("Welcome Email")
                .subject("Welcome {{name}}!")
                .body("Hello {{name}}, welcome to our service!")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .build();

        createRequest = TemplateDto.CreateRequest.builder()
                .code("welcome-email")
                .name("Welcome Email")
                .subject("Welcome {{name}}!")
                .body("Hello {{name}}, welcome to our service!")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .build();
    }

    @Test
    void createTemplate_shouldSucceed() {
        when(templateRepository.existsByCode("welcome-email")).thenReturn(false);
        when(templateRepository.save(any())).thenReturn(template);

        TemplateDto.Response response = templateService.createTemplate(createRequest);

        assertNotNull(response);
        assertEquals("welcome-email", response.getCode());
        assertEquals("Welcome Email", response.getName());
        verify(templateRepository).save(any());
    }

    @Test
    void createTemplate_shouldThrowOnDuplicate() {
        when(templateRepository.existsByCode("welcome-email")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> templateService.createTemplate(createRequest));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void getTemplate_shouldSucceed() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        TemplateDto.Response response = templateService.getTemplate(1L);

        assertNotNull(response);
        assertEquals("welcome-email", response.getCode());
    }

    @Test
    void getTemplate_shouldThrowOnNotFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplate(999L));
    }

    @Test
    void getTemplateByCode_shouldSucceed() {
        when(templateRepository.findByCode("welcome-email")).thenReturn(Optional.of(template));

        TemplateDto.Response response = templateService.getTemplateByCode("welcome-email");

        assertNotNull(response);
        assertEquals("welcome-email", response.getCode());
    }

    @Test
    void updateTemplate_shouldSucceed() {
        TemplateDto.UpdateRequest updateRequest = TemplateDto.UpdateRequest.builder()
                .name("Updated Name")
                .build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any())).thenReturn(template);

        TemplateDto.Response response = templateService.updateTemplate(1L, updateRequest);

        assertNotNull(response);
        verify(templateRepository).save(any());
    }

    @Test
    void deleteTemplate_shouldSucceed() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(templateRepository).deleteById(1L);

        templateService.deleteTemplate(1L);

        verify(templateRepository).deleteById(1L);
    }

    @Test
    void deleteTemplate_shouldThrowOnNotFound() {
        when(templateRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> templateService.deleteTemplate(999L));
    }

    @Test
    void renderTemplate_shouldReplaceVariables() {
        Map<String, String> variables = Map.of("name", "John");

        String result = templateService.renderTemplate(template, variables);

        assertEquals("Hello John, welcome to our service!", result);
    }

    @Test
    void renderTemplate_shouldReturnEmptyOnNullTemplate() {
        String result = templateService.renderTemplate(null, Map.of());
        assertEquals("", result);
    }

    @Test
    void renderSubject_shouldReplaceVariables() {
        Map<String, String> variables = Map.of("name", "John");

        String result = templateService.renderSubject(template, variables);

        assertEquals("Welcome John!", result);
    }
}
