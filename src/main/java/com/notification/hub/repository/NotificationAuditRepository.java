package com.notification.hub.repository;

import com.notification.hub.entity.NotificationAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, Long> {

    List<NotificationAudit> findByNotificationId(Long notificationId);

    Page<NotificationAudit> findByNotificationIdOrderByCreatedAtDesc(Long notificationId, Pageable pageable);
}
