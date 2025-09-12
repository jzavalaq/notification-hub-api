package com.notificationhub.controller;

import com.notificationhub.dto.PreferenceDto;
import com.notificationhub.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user notification preferences.
 * <p>
 * Allows users to configure their notification preferences including
 * enabled channels, opted-out types, and quiet hours.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "Preferences", description = "User notification preferences")
@Slf4j
public class PreferenceController {

    private final PreferenceService preferenceService;

    /**
     * Creates notification preferences for a user.
     *
     * @param request the preference creation request
     * @return the created preferences
     */
    @PostMapping
    @Operation(summary = "Create user notification preferences")
    @ApiResponse(responseCode = "201", description = "Preferences created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Preferences already exist for user")
    public ResponseEntity<PreferenceDto.Response> createPreference(@Valid @RequestBody PreferenceDto.CreateRequest request) {
        log.info("Creating preferences for user: {}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(preferenceService.createPreference(request));
    }

    /**
     * Retrieves notification preferences for a user.
     *
     * @param userId the user ID
     * @return the user preferences
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user preferences")
    @ApiResponse(responseCode = "200", description = "Preferences found")
    @ApiResponse(responseCode = "404", description = "Preferences not found")
    public ResponseEntity<PreferenceDto.Response> getPreference(@PathVariable String userId) {
        log.debug("Fetching preferences for user: {}", userId);
        return ResponseEntity.ok(preferenceService.getPreference(userId));
    }

    /**
     * Retrieves user preferences or returns default values if not found.
     *
     * @param userId the user ID
     * @return the user preferences or defaults
     */
    @GetMapping("/{userId}/or-default")
    @Operation(summary = "Get user preferences or default")
    @ApiResponse(responseCode = "200", description = "Preferences or defaults returned")
    public ResponseEntity<PreferenceDto.Response> getPreferenceOrDefault(@PathVariable String userId) {
        log.debug("Fetching preferences or defaults for user: {}", userId);
        return ResponseEntity.ok(preferenceService.getPreferenceOrDefault(userId));
    }

    /**
     * Updates notification preferences for a user.
     *
     * @param userId the user ID
     * @param request the update request
     * @return the updated preferences
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update user preferences")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    @ApiResponse(responseCode = "404", description = "Preferences not found")
    public ResponseEntity<PreferenceDto.Response> updatePreference(
            @PathVariable String userId,
            @Valid @RequestBody PreferenceDto.UpdateRequest request) {
        log.info("Updating preferences for user: {}", userId);
        return ResponseEntity.ok(preferenceService.updatePreference(userId, request));
    }

    /**
     * Deletes notification preferences for a user.
     *
     * @param userId the user ID
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user preferences")
    @ApiResponse(responseCode = "204", description = "Preferences deleted successfully")
    @ApiResponse(responseCode = "404", description = "Preferences not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(@PathVariable String userId) {
        log.info("Deleting preferences for user: {}", userId);
        preferenceService.deletePreference(userId);
    }
}
