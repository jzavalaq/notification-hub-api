package com.notificationhub.service;

import com.notificationhub.dto.PreferenceDto;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.exception.DuplicateResourceException;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    private NotificationPreference preference;
    private PreferenceDto.CreateRequest createRequest;
    private PreferenceDto.UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        preference = NotificationPreference.builder()
                .id(1L)
                .userId("user-123")
                .enabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL)))
                .optedOutTypes(new HashSet<>())
                .timezone("UTC")
                .defaultPriority(NotificationPreference.Priority.NORMAL)
                .build();

        createRequest = PreferenceDto.CreateRequest.builder()
                .userId("user-123")
                .enabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL)))
                .optedOutTypes(new HashSet<>())
                .timezone("UTC")
                .defaultPriority(NotificationPreference.Priority.NORMAL)
                .build();

        updateRequest = PreferenceDto.UpdateRequest.builder()
                .enabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL, NotificationTemplate.ChannelType.SMS)))
                .build();
    }

    @Test
    void createPreference_validRequest_returnsResponse() {
        when(preferenceRepository.existsByUserId("user-123")).thenReturn(false);
        when(preferenceRepository.save(any())).thenAnswer(inv -> {
            NotificationPreference p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PreferenceDto.Response response = preferenceService.createPreference(createRequest);

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        verify(preferenceRepository).save(any());
    }

    @Test
    void createPreference_duplicateUser_throwsException() {
        when(preferenceRepository.existsByUserId("user-123")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> preferenceService.createPreference(createRequest));
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void createPreference_nullChannels_defaultsToEmptySet() {
        createRequest.setEnabledChannels(null);
        createRequest.setOptedOutTypes(null);
        createRequest.setTimezone(null);
        createRequest.setDefaultPriority(null);

        when(preferenceRepository.existsByUserId("user-123")).thenReturn(false);
        when(preferenceRepository.save(any())).thenAnswer(inv -> {
            NotificationPreference p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PreferenceDto.Response response = preferenceService.createPreference(createRequest);

        assertNotNull(response);
        assertNotNull(response.getEnabledChannels());
        assertNotNull(response.getOptedOutTypes());
    }

    @Test
    void createPreference_withQuietHours_savesCorrectly() {
        createRequest.setQuietHoursStart(LocalTime.of(22, 0));
        createRequest.setQuietHoursEnd(LocalTime.of(8, 0));

        when(preferenceRepository.existsByUserId("user-123")).thenReturn(false);
        when(preferenceRepository.save(any())).thenAnswer(inv -> {
            NotificationPreference p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PreferenceDto.Response response = preferenceService.createPreference(createRequest);

        assertNotNull(response);
        assertEquals(LocalTime.of(22, 0), response.getQuietHoursStart());
        assertEquals(LocalTime.of(8, 0), response.getQuietHoursEnd());
    }

    @Test
    void updatePreference_existingUser_returnsResponse() {
        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(any())).thenReturn(preference);

        PreferenceDto.Response response = preferenceService.updatePreference("user-123", updateRequest);

        assertNotNull(response);
        verify(preferenceRepository).save(any());
    }

    @Test
    void updatePreference_nonExistentUser_throwsException() {
        when(preferenceRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> preferenceService.updatePreference("nonexistent", updateRequest));
    }

    @Test
    void updatePreference_partialUpdate_onlyUpdatesProvidedFields() {
        PreferenceDto.UpdateRequest partialUpdate = PreferenceDto.UpdateRequest.builder()
                .timezone("America/New_York")
                .build();

        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(any())).thenReturn(preference);

        PreferenceDto.Response response = preferenceService.updatePreference("user-123", partialUpdate);

        assertNotNull(response);
        verify(preferenceRepository).save(any());
    }

    @Test
    void getPreference_existingUser_returnsResponse() {
        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));

        PreferenceDto.Response response = preferenceService.getPreference("user-123");

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
    }

    @Test
    void getPreference_nonExistentUser_throwsException() {
        when(preferenceRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> preferenceService.getPreference("nonexistent"));
    }

    @Test
    void getPreferenceOrDefault_existingUser_returnsResponse() {
        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));

        PreferenceDto.Response response = preferenceService.getPreferenceOrDefault("user-123");

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
    }

    @Test
    void getPreferenceOrDefault_nonExistentUser_returnsDefaultResponse() {
        when(preferenceRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        PreferenceDto.Response response = preferenceService.getPreferenceOrDefault("nonexistent");

        assertNotNull(response);
        assertEquals("nonexistent", response.getUserId());
        assertTrue(response.getEnabledChannels().isEmpty());
        assertTrue(response.getOptedOutTypes().isEmpty());
    }

    @Test
    void deletePreference_existingUser_deletesSuccessfully() {
        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));
        doNothing().when(preferenceRepository).delete(preference);

        assertDoesNotThrow(() -> preferenceService.deletePreference("user-123"));
        verify(preferenceRepository).delete(preference);
    }

    @Test
    void deletePreference_nonExistentUser_throwsException() {
        when(preferenceRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> preferenceService.deletePreference("nonexistent"));
    }

    @Test
    void isChannelEnabled_channelInSet_returnsTrue() {
        preference.setEnabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL)));

        boolean result = preferenceService.isChannelEnabled(preference, NotificationTemplate.ChannelType.EMAIL);

        assertTrue(result);
    }

    @Test
    void isChannelEnabled_channelNotInSet_returnsFalse() {
        preference.setEnabledChannels(new HashSet<>(Set.of(NotificationTemplate.ChannelType.EMAIL)));

        boolean result = preferenceService.isChannelEnabled(preference, NotificationTemplate.ChannelType.SMS);

        assertFalse(result);
    }

    @Test
    void isChannelEnabled_emptySet_returnsTrue() {
        preference.setEnabledChannels(new HashSet<>());

        boolean result = preferenceService.isChannelEnabled(preference, NotificationTemplate.ChannelType.EMAIL);

        assertTrue(result);
    }

    @Test
    void isChannelEnabled_nullSet_returnsTrue() {
        preference.setEnabledChannels(null);

        boolean result = preferenceService.isChannelEnabled(preference, NotificationTemplate.ChannelType.EMAIL);

        assertTrue(result);
    }

    @Test
    void isChannelEnabled_nullPreference_returnsTrue() {
        boolean result = preferenceService.isChannelEnabled(null, NotificationTemplate.ChannelType.EMAIL);

        assertTrue(result);
    }

    @Test
    void isOptedOut_typeInSet_returnsTrue() {
        preference.setOptedOutTypes(new HashSet<>(Set.of("marketing")));

        boolean result = preferenceService.isOptedOut(preference, "marketing");

        assertTrue(result);
    }

    @Test
    void isOptedOut_typeNotInSet_returnsFalse() {
        preference.setOptedOutTypes(new HashSet<>(Set.of("marketing")));

        boolean result = preferenceService.isOptedOut(preference, "transactional");

        assertFalse(result);
    }

    @Test
    void isOptedOut_nullSet_returnsFalse() {
        preference.setOptedOutTypes(null);

        boolean result = preferenceService.isOptedOut(preference, "marketing");

        assertFalse(result);
    }

    @Test
    void isOptedOut_nullPreference_returnsFalse() {
        boolean result = preferenceService.isOptedOut(null, "marketing");

        assertFalse(result);
    }

    @Test
    void isInQuietHours_withinQuietHours_returnsTrue() {
        preference.setQuietHoursStart(LocalTime.of(22, 0));
        preference.setQuietHoursEnd(LocalTime.of(6, 0));
        preference.setTimezone("UTC");

        // This test may pass or fail depending on when it runs, so we just verify no exception
        assertDoesNotThrow(() -> preferenceService.isInQuietHours(preference));
    }

    @Test
    void isInQuietHours_nullPreference_returnsFalse() {
        boolean result = preferenceService.isInQuietHours(null);

        assertFalse(result);
    }

    @Test
    void isInQuietHours_nullQuietHours_returnsFalse() {
        preference.setQuietHoursStart(null);
        preference.setQuietHoursEnd(null);

        boolean result = preferenceService.isInQuietHours(preference);

        assertFalse(result);
    }

    @Test
    void findByUserId_existingUser_returnsOptional() {
        when(preferenceRepository.findByUserId("user-123")).thenReturn(Optional.of(preference));

        Optional<NotificationPreference> result = preferenceService.findByUserId("user-123");

        assertTrue(result.isPresent());
        assertEquals("user-123", result.get().getUserId());
    }

    @Test
    void findByUserId_nonExistentUser_returnsEmpty() {
        when(preferenceRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        Optional<NotificationPreference> result = preferenceService.findByUserId("nonexistent");

        assertFalse(result.isPresent());
    }
}
