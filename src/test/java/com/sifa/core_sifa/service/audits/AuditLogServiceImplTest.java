package com.sifa.core_sifa.service.audits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private IAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, objectMapper);
    }

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
        given(auditLogRepository.findTopByOrderByIdAuditLogDesc(any())).willReturn(List.of());
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
    void registrarLog_cuandoError_propagaExcepcion() {
        given(auditLogRepository.findTopByOrderByIdAuditLogDesc(any())).willReturn(List.of());
        var request = AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .detalles(Map.of("estado", "APROBADA"))
                .build();

        given(auditLogRepository.save(any())).willThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> auditLogService.registrarLog(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    @Test
    void registrarLog_encadenaConElHashAnterior() {
        // Último log con hashActual X; el nuevo debe apuntar su hashAnterior a X
        var ultimo = createAuditLog();
        String hashUltimo = "hash-del-log-anterior";
        ultimo.setHashActual(hashUltimo);
        given(auditLogRepository.findTopByOrderByIdAuditLogDesc(any())).willReturn(List.of(ultimo));

        var request = AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion("EVIDENCIA_VERIFICAR")
                .tablaAfectada("evidencias_fotograficas")
                .detalles(Map.of("idEvidencia", "1"))
                .build();

        auditLogService.registrarLog(request);

        verify(auditLogRepository).save(any(AuditLog.class));
        // Verificar que el hashAnterior quedó enlazado y que el hashActual no es nulo
        // Se captura el log para validar el encadenamiento
        var captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getHashAnterior()).isEqualTo(hashUltimo);
        assertThat(captor.getValue().getHashActual()).isNotBlank();
    }

    @Test
    void verificarCadena_cadenaIntegra_noReportaErrores() {
        var log1 = createAuditLog();
        var log2 = createAuditLog();
        log2.setIdAuditLog(2L);

        // Construir cadena válida computando hashes en orden
        log1.setHashAnterior(null);
        log1.setHashActual(auditLogService.calcularHash(log1));
        log2.setHashAnterior(log1.getHashActual());
        log2.setHashActual(auditLogService.calcularHash(log2));

        given(auditLogRepository.findAllOrderByIdAsc()).willReturn(List.of(log1, log2));

        var resultado = auditLogService.verificarCadena();

        assertThat(resultado.integra()).isTrue();
        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.totalEventos()).isEqualTo(2);
    }

    @Test
    void verificarCadena_contenidoAlterado_reportaError() {
        var log1 = createAuditLog();
        log1.setHashAnterior(null);
        log1.setHashActual(auditLogService.calcularHash(log1));

        // Se modifica un campo después de calcular el hash -> la cadena se rompe
        log1.setEmailUsuario("otro@test.cl");

        given(auditLogRepository.findAllOrderByIdAsc()).willReturn(List.of(log1));

        var resultado = auditLogService.verificarCadena();

        assertThat(resultado.integra()).isFalse();
        assertThat(resultado.errores()).isNotEmpty();
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
