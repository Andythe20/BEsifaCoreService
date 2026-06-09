package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface INotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findBySentAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<NotificationLog> findByTargetType(String targetType, Pageable pageable);
}
