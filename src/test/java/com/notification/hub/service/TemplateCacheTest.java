package com.notification.hub.service;

import com.notification.hub.dto.TemplateDto;
import com.notification.hub.entity.NotificationTemplate;
import com.notification.hub.exception.ResourceNotFoundException;
import com.notification.hub.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for template caching behavior.
 *
 * <p>These tests verify that the {@link org.springframework.cache.CacheManager} correctly caches
 * template lookups and evicts cache entries on updates/deletes.</p>
 *
 * <p>Uses in-memory ConcurrentMapCacheManager (Redis disabled in dev profile).</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class TemplateCacheTest {

    @MockBean
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    private NotificationTemplate createTemplate(String code, Long id) {
        return NotificationTemplate.builder()
                .id(id)
                .code(code)
                .name("Test Template")
                .subject("Test Subject")
                .body("Test Body")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        reset(templateRepository);
    }

    @Test
    @DisplayName("Cache hit should skip repository call")
    void cacheHit_shouldSkipRepositoryCall() {
        // Given: a template that exists in the repository
        String templateCode = "WELCOME_EMAIL";
        NotificationTemplate template = createTemplate(templateCode, 1L);

        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.of(template));

        // When: we call getTemplateByCode twice
        TemplateDto.Response firstCall = templateService.getTemplateByCode(templateCode);
        TemplateDto.Response secondCall = templateService.getTemplateByCode(templateCode);

        // Then: repository should only be called once (second call hits cache)
        verify(templateRepository, times(1)).findByCode(templateCode);

        // And: both responses should be equal
        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(firstCall.getCode(), secondCall.getCode());
        assertEquals(firstCall.getName(), secondCall.getName());
    }

    @Test
    @DisplayName("Cache miss should call repository")
    void cacheMiss_shouldCallRepository() {
        // Given: a template that doesn't exist
        String templateCode = "NONEXISTENT";
        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.empty());

        // When/Then: calling getTemplateByCode should throw and call repository
        assertThrows(ResourceNotFoundException.class, () ->
            templateService.getTemplateByCode(templateCode));

        verify(templateRepository, times(1)).findByCode(templateCode);
    }

    @Test
    @DisplayName("Different template codes should have separate cache entries")
    void differentCodes_shouldHaveSeparateCacheEntries() {
        // Given: two different templates
        NotificationTemplate template1 = createTemplate("TEMPLATE_1", 1L);
        NotificationTemplate template2 = createTemplate("TEMPLATE_2", 2L);

        when(templateRepository.findByCode("TEMPLATE_1"))
                .thenReturn(Optional.of(template1));
        when(templateRepository.findByCode("TEMPLATE_2"))
                .thenReturn(Optional.of(template2));

        // When: we call getTemplateByCode for both templates
        TemplateDto.Response response1 = templateService.getTemplateByCode("TEMPLATE_1");
        TemplateDto.Response response2 = templateService.getTemplateByCode("TEMPLATE_2");

        // Then: both should be retrieved from repository (different cache keys)
        verify(templateRepository, times(1)).findByCode("TEMPLATE_1");
        verify(templateRepository, times(1)).findByCode("TEMPLATE_2");

        assertEquals("TEMPLATE_1", response1.getCode());
        assertEquals("TEMPLATE_2", response2.getCode());
    }

    @Test
    @DisplayName("Create template should evict cache")
    void createTemplate_shouldEvictCache() {
        // Given: an existing template in cache
        String existingCode = "EXISTING_TEMPLATE";
        NotificationTemplate existingTemplate = createTemplate(existingCode, 1L);
        when(templateRepository.findByCode(existingCode))
                .thenReturn(Optional.of(existingTemplate));

        // Populate the cache
        templateService.getTemplateByCode(existingCode);
        verify(templateRepository, times(1)).findByCode(existingCode);

        // When: we create a new template (should evict all templates cache)
        TemplateDto.CreateRequest createRequest = TemplateDto.CreateRequest.builder()
                .code("NEW_TEMPLATE")
                .name("New Template")
                .subject("New Subject")
                .body("New Body")
                .channel(NotificationTemplate.ChannelType.EMAIL)
                .language("en")
                .build();

        NotificationTemplate newTemplate = createTemplate("NEW_TEMPLATE", 2L);
        when(templateRepository.existsByCode("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(newTemplate);

        templateService.createTemplate(createRequest);

        // And: we call getTemplateByCode again for the existing template
        reset(templateRepository);
        when(templateRepository.findByCode(existingCode))
                .thenReturn(Optional.of(existingTemplate));

        templateService.getTemplateByCode(existingCode);

        // Then: repository should be called again (cache was evicted)
        verify(templateRepository, times(1)).findByCode(existingCode);
    }

    @Test
    @DisplayName("Update template should evict cache")
    void updateTemplate_shouldEvictCache() {
        // Given: an existing template
        Long templateId = 1L;
        String templateCode = "UPDATE_TEST";
        NotificationTemplate template = createTemplate(templateCode, templateId);

        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.of(template));
        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        // Populate the cache
        templateService.getTemplateByCode(templateCode);
        verify(templateRepository, times(1)).findByCode(templateCode);

        // When: we update the template
        NotificationTemplate updatedTemplate = createTemplate(templateCode, templateId);
        updatedTemplate.setName("Updated Name");
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(updatedTemplate);

        TemplateDto.UpdateRequest updateRequest = new TemplateDto.UpdateRequest();
        updateRequest.setName("Updated Name");
        templateService.updateTemplate(templateId, updateRequest);

        // And: we call getTemplateByCode again
        reset(templateRepository);
        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.of(updatedTemplate));

        templateService.getTemplateByCode(templateCode);

        // Then: repository should be called again (cache was evicted)
        verify(templateRepository, times(1)).findByCode(templateCode);
    }

    @Test
    @DisplayName("Delete template should evict cache")
    void deleteTemplate_shouldEvictCache() {
        // Given: an existing template in cache
        Long templateId = 1L;
        String templateCode = "DELETE_TEST";
        NotificationTemplate template = createTemplate(templateCode, templateId);

        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.of(template));

        // Populate the cache
        templateService.getTemplateByCode(templateCode);
        verify(templateRepository, times(1)).findByCode(templateCode);

        // When: we delete the template
        when(templateRepository.existsById(templateId)).thenReturn(true);
        doNothing().when(templateRepository).deleteById(templateId);
        templateService.deleteTemplate(templateId);

        // And: we call getTemplateByCode again (should call repository since cache was evicted)
        reset(templateRepository);
        when(templateRepository.findByCode(templateCode))
                .thenReturn(Optional.empty());

        // Then: should throw since template was deleted
        assertThrows(ResourceNotFoundException.class, () ->
            templateService.getTemplateByCode(templateCode));

        verify(templateRepository, times(1)).findByCode(templateCode);
    }
}
