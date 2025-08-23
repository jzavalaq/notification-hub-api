package com.notificationhub.controller;

import com.notificationhub.dto.PageResponse;
import com.notificationhub.dto.TemplateDto;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Notification template management")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a new notification template")
    public ResponseEntity<TemplateDto.Response> createTemplate(@Valid @RequestBody TemplateDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<TemplateDto.Response> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplate(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get template by code")
    public ResponseEntity<TemplateDto.Response> getTemplateByCode(@PathVariable String code) {
        return ResponseEntity.ok(templateService.getTemplateByCode(code));
    }

    @GetMapping
    @Operation(summary = "List all templates")
    public ResponseEntity<PageResponse<TemplateDto.Response>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationTemplate.TemplateStatus status) {
        return ResponseEntity.ok(templateService.getAllTemplates(page, size, status));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a template")
    public ResponseEntity<TemplateDto.Response> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateDto.UpdateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
    }
}
