package com.notification.hub.repository;

import com.notification.hub.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookId(Long webhookId);

    List<WebhookDelivery> findByStatus(WebhookDelivery.DeliveryStatus status);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(WebhookDelivery.DeliveryStatus status, java.time.Instant nextRetryAt);
}
