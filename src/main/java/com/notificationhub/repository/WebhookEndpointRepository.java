package com.notificationhub.repository;

import com.notificationhub.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    List<WebhookEndpoint> findByUserId(String userId);

    List<WebhookEndpoint> findByStatus(WebhookEndpoint.WebhookStatus status);

    List<WebhookEndpoint> findBySubscribedEventsContaining(WebhookEndpoint.WebhookEventType eventType);
}
