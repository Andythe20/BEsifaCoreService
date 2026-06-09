package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.model.NotificationLog;
import com.sifa.core_sifa.service.notification.INotificationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/core/api/v1/notifications/history")
@RequiredArgsConstructor
@Tag(name = "Notification History", description = "Historial de notificaciones push enviadas")
public class NotificationLogController {

    private final INotificationLogService notificationLogService;

    @Operation(summary = "Obtener historial de notificaciones",
            description = "Retorna las notificaciones enviadas, con paginación y filtros opcionales por tipo y fecha")
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @GetMapping
    public ResponseEntity<Page<NotificationLog>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<NotificationLog> history = notificationLogService.getHistory(targetType, start, end, pageable);
        return ResponseEntity.ok(history);
    }
}
