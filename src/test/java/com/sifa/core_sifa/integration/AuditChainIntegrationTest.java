package com.sifa.core_sifa.integration;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;
import com.sifa.core_sifa.service.audits.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditChainIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogServiceImpl auditLogService;

    @Autowired
    private IAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void registrarVariosLogs_cadenaIntegra_noReportaErrores() {
        registrarLog("LOGIN", Map.of("rol", "USER_ADMIN", "Estado", "Exitoso"));
        registrarLog("USUARIO_CREADO", Map.of("nombre", "Admin", "Estado", "Exitoso"));
        registrarLog("EVIDENCIA_REGISTRAR", Map.of("idEvidencia", "101", "sha256", "abc123"));

        var resultado = auditLogService.verificarCadena();

        assertThat(resultado.totalEventos()).isEqualTo(3);
        assertThat(resultado.integra()).isTrue();
        assertThat(resultado.errores()).isEmpty();
    }

    @Test
    void contenidoManipulado_rompeLaCadena() {
        registrarLog("LOGIN", Map.of("rol", "USER_ADMIN", "Estado", "Exitoso"));
        registrarLog("EVIDENCIA_REGISTRAR", Map.of("idEvidencia", "101"));

        // Se altera el contenido de un log directamente en la BD
        AuditLog log = auditLogRepository.findAllOrderByIdAsc().getFirst();
        log.setEmailUsuario("atacante@test.cl");
        auditLogRepository.save(log);

        var resultado = auditLogService.verificarCadena();

        assertThat(resultado.totalEventos()).isEqualTo(2);
        assertThat(resultado.integra()).isFalse();
        assertThat(resultado.errores()).isNotEmpty();
    }

    private void registrarLog(String accion, Map<String, Object> detalles) {
        auditLogService.registrarLog(AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion(accion)
                .tablaAfectada("AUDIT_LOGS")
                .idRegistroAfectado("1")
                .detalles(detalles)
                .build());
    }
}