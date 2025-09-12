package com.notificationhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a notification message.
 * <p>
 * Stores notification details including recipient, content, status,
 * and delivery tracking information across multiple channels.
 * </p>
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id", columnList = "user_id"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_channel", columnList = "channel"),
    @Index(name = "idx_notifications_created_at", columnList = "createdAt"),
    @Index(name = "idx_notifications_scheduled_at", columnList = "scheduledAt"),
    @Index(name = "idx_notifications_status_retry", columnList = "status, retryCount")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTemplate.ChannelType channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer retryCount;

    @Builder.Default
    private Integer maxRetries = 3;

    private Instant scheduledAt;

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @ElementCollection
    @CollectionTable(name = "notification_metadata", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationPreference.Priority priority = NotificationPreference.Priority.NORMAL;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (retryCount == null) retryCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum NotificationStatus {
        PENDING, QUEUED, SENT, DELIVERED, FAILED, CANCELLED, READ
    }
}
