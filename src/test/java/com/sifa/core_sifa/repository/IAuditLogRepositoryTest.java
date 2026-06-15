package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IAuditLogRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        auditLogRepository.save(AuditLog.builder()
                .emailUsuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .tablaAfectada("INFRACCIONES")
                .idRegistroAfectado("1")
                .detalles(Map.of("estado", "APROBADA"))
                .fechaHora(LocalDateTime.now())
                .build());
        auditLogRepository.save(AuditLog.builder()
                .emailUsuario("supervisor@test.cl")
                .accion("CREAR_INFRACCION")
                .tablaAfectada("INFRACCIONES")
                .idRegistroAfectado("2")
                .detalles(Map.of("tipo", "MAL_ESTACIONADO"))
                .fechaHora(LocalDateTime.now().minusDays(1))
                .build());
    }

    @Test
    void findByFilters_withAllParams_returnsFiltered() {
        var start = LocalDate.now().minusDays(1).atStartOfDay();
        var end = LocalDate.now().atTime(LocalTime.MAX);

        var result = auditLogRepository.findByFilters(start, end, "admin@test.cl", null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getEmailUsuario()).isEqualTo("admin@test.cl");
    }

    @Test
    void findByFilters_withSearch_returnsMatching() {
        var result = auditLogRepository.findByFilters(null, null, null, "CREAR", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getAccion()).isEqualTo("CREAR_INFRACCION");
    }

    @Test
    void findByFilters_withoutFilters_returnsAll() {
        var result = auditLogRepository.findByFilters(null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByFilters_withUserNoMatch_returnsEmpty() {
        var result = auditLogRepository.findByFilters(null, null, "unknown@test.cl", null, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}
