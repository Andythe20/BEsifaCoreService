package com.sifa.core_sifa.service.notification;

import com.sifa.core_sifa.model.NotificationLog;
import com.sifa.core_sifa.repository.INotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceImplTest {

    @Mock
    private INotificationLogRepository repository;

    @InjectMocks
    private NotificationLogServiceImpl notificationLogService;

    @Captor
    private ArgumentCaptor<NotificationLog> logCaptor;

    @Test
    void log_guardaEntrada() {
        notificationLogService.log("ALL", "Test Title", "Test Body",
                "1.0.0", "ANDROID", 10, "admin@test.cl");

        verify(repository).save(logCaptor.capture());

        var saved = logCaptor.getValue();
        assertThat(saved.getTargetType()).isEqualTo("ALL");
        assertThat(saved.getTitle()).isEqualTo("Test Title");
        assertThat(saved.getDevicesCount()).isEqualTo(10);
        assertThat(saved.getSentBy()).isEqualTo("admin@test.cl");
    }

    @Test
    void getHistory_withTargetType_filtersByType() {
        var log = NotificationLog.builder().targetType("ALL").title("Test").build();
        var page = new PageImpl<>(List.of(log));
        given(repository.findByTargetType(eq("ALL"), any(PageRequest.class))).willReturn(page);

        var result = notificationLogService.getHistory("ALL", null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getTargetType()).isEqualTo("ALL");
    }

    @Test
    void getHistory_withDateRange_filtersByDate() {
        var log = NotificationLog.builder().targetType("ALL").title("Test").build();
        var page = new PageImpl<>(List.of(log));
        var start = LocalDateTime.of(2024, 1, 1, 0, 0);
        var end = LocalDateTime.of(2024, 12, 31, 23, 59);
        given(repository.findBySentAtBetween(eq(start), eq(end), any(PageRequest.class))).willReturn(page);

        var result = notificationLogService.getHistory(null, start, end, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void getHistory_withoutFilters_returnsAll() {
        var log = NotificationLog.builder().targetType("ALL").title("Test").build();
        var page = new PageImpl<>(List.of(log));
        given(repository.findAll(any(PageRequest.class))).willReturn(page);

        var result = notificationLogService.getHistory(null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }
}
