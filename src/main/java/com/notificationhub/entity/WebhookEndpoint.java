package com.notificationhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "webhook_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secret;

    @ElementCollection
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    @Builder.Default
    private Set<WebhookEventType> subscribedEvents = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.ACTIVE;

    @Builder.Default
    private Integer maxRetries = 5;

    @Builder.Default
    private Integer timeoutSeconds = 30;

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

    public enum WebhookEventType {
        NOTIFICATION_SENT, NOTIFICATION_DELIVERED, NOTIFICATION_FAILED, NOTIFICATION_READ
    }

    public enum WebhookStatus {
        ACTIVE, INACTIVE, DISABLED
    }
}
