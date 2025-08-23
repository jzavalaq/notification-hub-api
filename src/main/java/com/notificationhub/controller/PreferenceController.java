package com.notificationhub.controller;

import com.notificationhub.dto.PreferenceDto;
import com.notificationhub.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "Preferences", description = "User notification preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PostMapping
    @Operation(summary = "Create user notification preferences")
    public ResponseEntity<PreferenceDto.Response> createPreference(@Valid @RequestBody PreferenceDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preferenceService.createPreference(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<PreferenceDto.Response> getPreference(@PathVariable String userId) {
        return ResponseEntity.ok(preferenceService.getPreference(userId));
    }

    @GetMapping("/{userId}/or-default")
    @Operation(summary = "Get user preferences or default")
    public ResponseEntity<PreferenceDto.Response> getPreferenceOrDefault(@PathVariable String userId) {
        return ResponseEntity.ok(preferenceService.getPreferenceOrDefault(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<PreferenceDto.Response> updatePreference(
            @PathVariable String userId,
            @Valid @RequestBody PreferenceDto.UpdateRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreference(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user preferences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(@PathVariable String userId) {
        preferenceService.deletePreference(userId);
    }
}
