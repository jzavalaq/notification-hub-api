package com.notificationhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @ElementCollection
    @CollectionTable(name = "preference_enabled_channels", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    @Builder.Default
    private Set<NotificationTemplate.ChannelType> enabledChannels = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "preference_opted_out_types", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "notification_type")
    @Builder.Default
    private Set<String> optedOutTypes = new HashSet<>();

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority defaultPriority = Priority.NORMAL;

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

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
}
