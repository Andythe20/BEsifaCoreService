package com.sifa.core_sifa.service.audits;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private IAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void findById_whenExists_returnsAuditLog() {
        var auditLog = createAuditLog();
        given(auditLogRepository.findById(1L)).willReturn(Optional.of(auditLog));

        var result = auditLogService.findById(1L);

        assertThat(result.getAccion()).isEqualTo("PROCESAR_INFRACCION");
        assertThat(result.getEmail_usuario()).isEqualTo("admin@test.cl");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        given(auditLogRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auditoría no encontrada");
    }

    @Test
    void findAll_withFilters_returnsPagedResults() {
        var auditLog = createAuditLog();
        var page = new PageImpl<>(List.of(auditLog));
        given(auditLogRepository.findByFilters(any(), any(), any(), any(), any()))
                .willReturn(page);

        var result = auditLogService.findAll(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "admin@test.cl",
                "infraccion",
                PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getAccion()).isEqualTo("PROCESAR_INFRACCION");
    }

    @Test
    void findAll_withoutFilters_returnsAll() {
        var auditLog = createAuditLog();
        var page = new PageImpl<>(List.of(auditLog));
        given(auditLogRepository.findByFilters(null, null, null, null, PageRequest.of(0, 10)))
                .willReturn(page);

        var result = auditLogService.findAll(null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void registrarLog_savesSuccessfully() {
        var request = AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .tablaAfectada("INFRACCIONES")
                .idRegistroAfectado("1")
                .detalles(Map.of("estado", "APROBADA"))
                .build();

        auditLogService.registrarLog(request);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void registrarLog_cuandoError_noPropagaExcepcion() {
        var request = AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .build();

        given(auditLogRepository.save(any())).willThrow(new RuntimeException("DB error"));

        auditLogService.registrarLog(request);

        // Debe atrapar el error silenciosamente sin propagar
        verify(auditLogRepository).save(any());
    }

    private AuditLog createAuditLog() {
        return AuditLog.builder()
                .idAuditLog(1L)
                .emailUsuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .tablaAfectada("INFRACCIONES")
                .idRegistroAfectado("1")
                .detalles(Map.of("estado", "APROBADA"))
                .fechaHora(LocalDateTime.now())
                .build();
    }
}
