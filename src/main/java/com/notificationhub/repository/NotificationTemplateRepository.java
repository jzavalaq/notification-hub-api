package com.notificationhub.repository;

import com.notificationhub.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationTemplate entity.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCode(String code);

    List<NotificationTemplate> findByChannel(NotificationTemplate.ChannelType channel);

    List<NotificationTemplate> findByLanguage(String language);

    List<NotificationTemplate> findByStatus(NotificationTemplate.TemplateStatus status);

    Page<NotificationTemplate> findByStatus(NotificationTemplate.TemplateStatus status, Pageable pageable);

    boolean existsByCode(String code);
}
