package com.notificationhub.repository;

import com.notificationhub.entity.Notification;
import com.notificationhub.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(String userId, Pageable pageable);

    Page<Notification> findByStatus(Notification.NotificationStatus status, Pageable pageable);

    List<Notification> findByStatusAndScheduledAtBefore(Notification.NotificationStatus status, Instant scheduledAt);

    List<Notification> findByStatusAndRetryCountLessThan(Notification.NotificationStatus status, Integer maxRetries);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = :status")
    List<Notification> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") Notification.NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status IN :statuses")
    Long countByUserIdAndStatuses(@Param("userId") String userId, @Param("statuses") List<Notification.NotificationStatus> statuses);

    @Query("SELECT n.channel, COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = :status GROUP BY n.channel")
    List<Object[]> countByUserIdAndStatusGroupByChannel(@Param("userId") String userId, @Param("status") Notification.NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.channel = :channel AND n.createdAt BETWEEN :start AND :end")
    Long countByChannelAndDateRange(@Param("channel") NotificationTemplate.ChannelType channel, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt BETWEEN :start AND :end")
    Long countByStatusAndDateRange(@Param("status") Notification.NotificationStatus status, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT n.channel, COUNT(n) FROM Notification n WHERE n.createdAt BETWEEN :start AND :end GROUP BY n.channel")
    List<Object[]> countGroupByChannel(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.createdAt BETWEEN :start AND :end GROUP BY n.status")
    List<Object[]> countGroupByStatus(@Param("start") Instant start, @Param("end") Instant end);
}
