package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.NotificationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class INotificationLogRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private INotificationLogRepository notificationLogRepository;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
        notificationLogRepository.save(NotificationLog.builder()
                .targetType("ALL").title("Test All").body("Body")
                .devicesCount(5).sentBy("admin@test.cl").sentAt(LocalDateTime.now())
                .build());
        notificationLogRepository.save(NotificationLog.builder()
                .targetType("SINGLE").title("Test Single").body("Body")
                .devicesCount(1).sentBy("admin@test.cl").sentAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    @Test
    void findBySentAtBetween_conRangoFuturo_retornaVacio() {
        var start = LocalDateTime.now().plusDays(1);
        var end = LocalDateTime.now().plusDays(2);

        var result = notificationLogRepository.findBySentAtBetween(start, end, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void findByTargetType_returnsFiltered() {
        var result = notificationLogRepository.findByTargetType("SINGLE", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getTargetType()).isEqualTo("SINGLE");
    }

    @Test
    void findByTargetType_cuandoNoExiste_retornaEmpty() {
        var result = notificationLogRepository.findByTargetType("UNKNOWN", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAll() {
        var result = notificationLogRepository.findAll(PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
    }
}
