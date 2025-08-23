package com.notificationhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "webhook_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    private WebhookEndpoint webhook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookEndpoint.WebhookEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Integer statusCode = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private String errorMessage;

    @Builder.Default
    private Integer attemptCount = 0;

    private Instant nextRetryAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deliveredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum DeliveryStatus {
        PENDING, DELIVERED, FAILED, RETRYING
    }
}
