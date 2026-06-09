package com.sifa.core_sifa.service.notification;

import com.sifa.core_sifa.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface INotificationLogService {

    void log(String targetType, String title, String body,
             String appVersion, String platform,
             Integer devicesCount, String sentBy);

    Page<NotificationLog> getHistory(String targetType,
                                     LocalDateTime startDate,
                                     LocalDateTime endDate,
                                     Pageable pageable);
}
