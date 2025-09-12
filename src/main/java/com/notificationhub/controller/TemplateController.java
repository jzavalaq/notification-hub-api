package com.notificationhub.controller;

import com.notificationhub.dto.PageResponse;
import com.notificationhub.dto.TemplateDto;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.service.TemplateService;
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
 * REST controller for managing notification templates.
 * <p>
 * Provides CRUD operations for notification templates that can be reused
 * across multiple notification types and channels.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Notification template management")
@Slf4j
public class TemplateController {

    private final TemplateService templateService;

    /**
     * Creates a new notification template.
     *
     * @param request the template creation request
     * @return the created template
     */
    @PostMapping
    @Operation(summary = "Create a new notification template")
    @ApiResponse(responseCode = "201", description = "Template created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Template with code already exists")
    public ResponseEntity<TemplateDto.Response> createTemplate(@Valid @RequestBody TemplateDto.CreateRequest request) {
        log.info("Creating template with code: {}", request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param id the template ID
     * @return the template
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    @ApiResponse(responseCode = "200", description = "Template found")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<TemplateDto.Response> getTemplate(@PathVariable Long id) {
        log.debug("Fetching template with id: {}", id);
        return ResponseEntity.ok(templateService.getTemplate(id));
    }

    /**
     * Retrieves a template by its unique code.
     *
     * @param code the template code
     * @return the template
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Get template by code")
    @ApiResponse(responseCode = "200", description = "Template found")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<TemplateDto.Response> getTemplateByCode(@PathVariable String code) {
        log.debug("Fetching template with code: {}", code);
        return ResponseEntity.ok(templateService.getTemplateByCode(code));
    }

    /**
     * Lists all templates with pagination support.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @param status optional status filter
     * @return paginated list of templates
     */
    @GetMapping
    @Operation(summary = "List all templates")
    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    public ResponseEntity<PageResponse<TemplateDto.Response>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationTemplate.TemplateStatus status) {
        log.debug("Listing templates - page: {}, size: {}, status: {}", page, size, status);
        return ResponseEntity.ok(templateService.getAllTemplates(page, size, status));
    }

    /**
     * Updates an existing template.
     *
     * @param id the template ID
     * @param request the update request
     * @return the updated template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a template")
    @ApiResponse(responseCode = "200", description = "Template updated successfully")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<TemplateDto.Response> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateDto.UpdateRequest request) {
        log.info("Updating template with id: {}", id);
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    /**
     * Deletes a template by ID.
     *
     * @param id the template ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    @ApiResponse(responseCode = "204", description = "Template deleted successfully")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        log.info("Deleting template with id: {}", id);
        templateService.deleteTemplate(id);
    }
}
