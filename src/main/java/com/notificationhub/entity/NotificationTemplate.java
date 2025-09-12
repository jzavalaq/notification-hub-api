package com.notificationhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a reusable notification template.
 * <p>
 * Templates support variable substitution using {{variableName}} syntax
 * and can be reused across multiple notifications.
 * </p>
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_notification_templates_code", columnList = "code"),
    @Index(name = "idx_notification_templates_channel", columnList = "channel"),
    @Index(name = "idx_notification_templates_status", columnList = "status"),
    @Index(name = "idx_notification_templates_language", columnList = "language")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channel;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.ACTIVE;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ChannelType {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum TemplateStatus {
        ACTIVE, INACTIVE, ARCHIVED
    }
}
