package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import org.springframework.transaction.annotation.Transactional;

import com.sifa.core_sifa.dto.audit.AuditLogResponseDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AuditLogResponseDTO findById(Long id) {
        log.info("Buscando auditoria con id: {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auditoría no encontrada o inexistente"));

        return AuditLogResponseDTO.fromEntity(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> findAllAuditLogs() {
        log.info("Listando todas las auditorias");
        return auditLogRepository.findAll()
                .stream()
                .map(AuditLogResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> findByEmailUsuario(String emailUsuario) {
        log.info("Listando todas las auditorias del usuario {}", emailUsuario);
        return auditLogRepository.findByEmailUsuario(emailUsuario)
                .stream()
                .map(AuditLogResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void registrarLog(AuditLogRequestDTO request) {
        AuditLog logEntity = AuditLog.builder()
                .emailUsuario(request.getEmailUsuario())
                .accion(request.getAccion())
                .tablaAfectada(request.getTablaAfectada())
                .idRegistroAfectado(request.getIdRegistroAfectado())
                .detalles(request.getDetalles())
                .build();

        auditLogRepository.save(logEntity);
        log.info("Auditoría guardada exitosamente | Usuario: {} | Acción: {}", request.getEmailUsuario(), request.getAccion());
    }


}
