package com.sifa.core_sifa.service.notification;

import com.sifa.core_sifa.model.NotificationLog;
import com.sifa.core_sifa.repository.INotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements INotificationLogService {

    private final INotificationLogRepository repository;

    @Override
    @Transactional
    public void log(String targetType, String title, String body,
                    String appVersion, String platform,
                    Integer devicesCount, String sentBy) {
        NotificationLog logEntry = NotificationLog.builder()
                .targetType(targetType)
                .title(title)
                .body(body)
                .appVersion(appVersion)
                .platform(platform)
                .devicesCount(devicesCount)
                .sentBy(sentBy)
                .build();

        repository.save(logEntry);
        log.info("Notification log saved: targetType={}, devicesCount={}, sentBy={}",
                targetType, devicesCount, sentBy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLog> getHistory(String targetType,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            Pageable pageable) {
        if (targetType != null && !targetType.isBlank()) {
            return repository.findByTargetType(targetType, pageable);
        }
        if (startDate != null && endDate != null) {
            return repository.findBySentAtBetween(startDate, endDate, pageable);
        }
        return repository.findAll(pageable);
    }
}
