package com.notificationhub.service;

import com.notificationhub.dto.PageResponse;
import com.notificationhub.dto.TemplateDto;
import com.notificationhub.entity.NotificationTemplate;
import com.notificationhub.exception.DuplicateResourceException;
import com.notificationhub.exception.ResourceNotFoundException;
import com.notificationhub.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");
    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public TemplateDto.Response createTemplate(TemplateDto.CreateRequest request) {
        if (templateRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Template", "code", request.getCode());
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .code(request.getCode())
                .name(request.getName())
                .subject(request.getSubject())
                .body(request.getBody())
                .channel(request.getChannel())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .status(NotificationTemplate.TemplateStatus.ACTIVE)
                .build();

        template = templateRepository.save(template);
        log.info("Created template with code: {}", template.getCode());

        return toResponse(template);
    }

    @Transactional
    public TemplateDto.Response updateTemplate(Long id, TemplateDto.UpdateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", id));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getSubject() != null) template.setSubject(request.getSubject());
        if (request.getBody() != null) template.setBody(request.getBody());
        if (request.getChannel() != null) template.setChannel(request.getChannel());
        if (request.getLanguage() != null) template.setLanguage(request.getLanguage());
        if (request.getStatus() != null) template.setStatus(request.getStatus());

        template = templateRepository.save(template);
        log.info("Updated template with id: {}", id);

        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public TemplateDto.Response getTemplate(Long id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", id));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public TemplateDto.Response getTemplateByCode(String code) {
        NotificationTemplate template = templateRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "code", code));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public PageResponse<TemplateDto.Response> getAllTemplates(int page, int size, NotificationTemplate.TemplateStatus status) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        if (page < 0) page = 0;
        if (safeSize < 1) safeSize = 10;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<NotificationTemplate> templates;

        if (status != null) {
            templates = templateRepository.findByStatus(status, pageable);
        } else {
            templates = templateRepository.findAll(pageable);
        }

        List<TemplateDto.Response> content = templates.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<TemplateDto.Response>builder()
                .content(content)
                .pageNumber(templates.getNumber())
                .pageSize(templates.getSize())
                .totalElements(templates.getTotalElements())
                .totalPages(templates.getTotalPages())
                .first(templates.isFirst())
                .last(templates.isLast())
                .build();
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Template", "id", id);
        }
        templateRepository.deleteById(id);
        log.info("Deleted template with id: {}", id);
    }

    public String renderTemplate(NotificationTemplate template, Map<String, String> variables) {
        if (template == null || template.getBody() == null) {
            return "";
        }

        String result = template.getBody();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    public String renderSubject(NotificationTemplate template, Map<String, String> variables) {
        if (template == null || template.getSubject() == null) {
            return null;
        }

        String result = template.getSubject();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    private TemplateDto.Response toResponse(NotificationTemplate template) {
        return TemplateDto.Response.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .subject(template.getSubject())
                .body(template.getBody())
                .channel(template.getChannel())
                .language(template.getLanguage())
                .status(template.getStatus())
                .version(template.getVersion())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
