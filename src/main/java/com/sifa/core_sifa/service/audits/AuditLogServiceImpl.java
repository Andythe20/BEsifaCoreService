package com.sifa.core_sifa.service.audits;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.sifa.core_sifa.dto.audit.AuditLogResponseDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private final IAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AuditLogResponseDTO findById(Long id) {
        log.info("Buscando auditoria con id: {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auditoría no encontrada o inexistente"));

        return AuditLogResponseDTO.fromEntity(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> findAll(
            LocalDate startDate,
            LocalDate endDate,
            String user,
            String search,
            Pageable pageable
    ) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDate != null) {
            start = startDate.atStartOfDay();
        }
        if (endDate != null) {
            end = endDate.atTime(23, 59, 59);
        }

        String searchQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<AuditLog> audits = auditLogRepository.findByFilters(
                start,
                end,
                user,
                searchQuery,
                pageable
        );

        return audits.map(AuditLogResponseDTO::fromEntity);
    }


    @Transactional
    public void registrarLog(AuditLogRequestDTO request) {
        try {
            log.debug("[AUDITORIA] Iniciando envío asíncrono -> Usuario: {} | Acción: {}", request.getEmailUsuario(), request.getAccion());
            AuditLog logEntity = AuditLog.builder()
                    .emailUsuario(request.getEmailUsuario())
                    .accion(request.getAccion())
                    .tablaAfectada(request.getTablaAfectada())
                    .idRegistroAfectado(request.getIdRegistroAfectado())
                    .detalles(request.getDetalles())
                    .build();

            auditLogRepository.save(logEntity);
            log.info("Auditoría guardada exitosamente | Usuario: {} | Acción: {}", request.getEmailUsuario(), request.getAccion());
        } catch (IllegalArgumentException e) {
            log.error("[AUDITORIA-ERROR] Fallo de validación de URI o argumentos.");
            log.error("Mensaje: {}", e.getMessage());
            log.error("Stacktrace:", e);
        } catch (Exception e) {
            // Atrapamos el error para que NO afecte al usuario.
            log.error("[AUDITORIA-ERROR] Error silencioso general al comunicarse con el Core.");
            log.error("Clase del error: {}", e.getClass().getName());
            log.error("Mensaje exacto: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Causa raíz (Cause): {}", e.getCause().toString());
            }
            // Imprime el stacktrace completo para ver exactamente en qué clase de Feign falló
            log.error("Stacktrace completo: ", e);
        }
    }


}
