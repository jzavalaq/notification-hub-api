package com.notificationhub.service;

import com.notificationhub.dto.PreferenceDto;
import com.notificationhub.entity.NotificationPreference;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.exception.DuplicateResourceException;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional
    public PreferenceDto.Response createPreference(PreferenceDto.CreateRequest request) {
        if (preferenceRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException("Preference", "userId", request.getUserId());
        }

        NotificationPreference preference = NotificationPreference.builder()
                .userId(request.getUserId())
                .enabledChannels(request.getEnabledChannels() != null ? request.getEnabledChannels() : new HashSet<>())
                .optedOutTypes(request.getOptedOutTypes() != null ? request.getOptedOutTypes() : new HashSet<>())
                .quietHoursStart(request.getQuietHoursStart())
                .quietHoursEnd(request.getQuietHoursEnd())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .defaultPriority(request.getDefaultPriority() != null ? request.getDefaultPriority() : NotificationPreference.Priority.NORMAL)
                .build();

        preference = preferenceRepository.save(preference);
        log.info("Created preference for user: {}", preference.getUserId());

        return toResponse(preference);
    }

    @Transactional
    public PreferenceDto.Response updatePreference(String userId, PreferenceDto.UpdateRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preference", "userId", userId));

        if (request.getEnabledChannels() != null) preference.setEnabledChannels(request.getEnabledChannels());
        if (request.getOptedOutTypes() != null) preference.setOptedOutTypes(request.getOptedOutTypes());
        if (request.getQuietHoursStart() != null) preference.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) preference.setQuietHoursEnd(request.getQuietHoursEnd());
        if (request.getTimezone() != null) preference.setTimezone(request.getTimezone());
        if (request.getDefaultPriority() != null) preference.setDefaultPriority(request.getDefaultPriority());

        preference = preferenceRepository.save(preference);
        log.info("Updated preference for user: {}", userId);

        return toResponse(preference);
    }

    @Transactional(readOnly = true)
    public PreferenceDto.Response getPreference(String userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preference", "userId", userId));
        return toResponse(preference);
    }

    @Transactional(readOnly = true)
    public PreferenceDto.Response getPreferenceOrDefault(String userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> PreferenceDto.Response.builder()
                        .userId(userId)
                        .enabledChannels(new HashSet<>())
                        .optedOutTypes(new HashSet<>())
                        .timezone("UTC")
                        .defaultPriority(NotificationPreference.Priority.NORMAL)
                        .build());
    }

    @Transactional
    public void deletePreference(String userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preference", "userId", userId));
        preferenceRepository.delete(preference);
        log.info("Deleted preference for user: {}", userId);
    }

    public boolean isChannelEnabled(NotificationPreference preference, NotificationTemplate.ChannelType channel) {
        if (preference == null || preference.getEnabledChannels() == null || preference.getEnabledChannels().isEmpty()) {
            return true; // All channels enabled by default
        }
        return preference.getEnabledChannels().contains(channel);
    }

    public boolean isOptedOut(NotificationPreference preference, String notificationType) {
        if (preference == null || preference.getOptedOutTypes() == null) {
            return false;
        }
        return preference.getOptedOutTypes().contains(notificationType);
    }

    public boolean isInQuietHours(NotificationPreference preference) {
        if (preference == null || preference.getQuietHoursStart() == null || preference.getQuietHoursEnd() == null) {
            return false;
        }

        try {
            ZoneId zoneId = ZoneId.of(preference.getTimezone());
            LocalTime now = ZonedDateTime.now(zoneId).toLocalTime();
            LocalTime start = preference.getQuietHoursStart();
            LocalTime end = preference.getQuietHoursEnd();

            if (start.isBefore(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                // Quiet hours span midnight
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            log.warn("Error checking quiet hours: {}", e.getMessage());
            return false;
        }
    }

    public Optional<NotificationPreference> findByUserId(String userId) {
        return preferenceRepository.findByUserId(userId);
    }

    private PreferenceDto.Response toResponse(NotificationPreference preference) {
        return PreferenceDto.Response.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .enabledChannels(preference.getEnabledChannels())
                .optedOutTypes(preference.getOptedOutTypes())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .timezone(preference.getTimezone())
                .defaultPriority(preference.getDefaultPriority())
                .version(preference.getVersion())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
