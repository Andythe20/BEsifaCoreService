package com.sifa.core_sifa.service;

import org.springframework.transaction.annotation.Transactional;

import com.sifa.core_sifa.dto.AuditLogDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.AuditLog;
import com.sifa.core_sifa.repository.IAuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final IAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AuditLogDTO findById(Integer id){
        log.info("Buscando auditoria con id: {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auditoría no encontrada o inexistente"));

        return AuditLogDTO.fromEntity(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findAllAuditLogs() {
        log.info("Listando todas las auditorias");
        return auditLogRepository.findAll()
                .stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByEmailUsuario(String emailUsuario) {
        log.info("Listando todas las auditorias del usuario {}", emailUsuario);
        return auditLogRepository.findByEmailUsuario(emailUsuario)
                .stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void crearAuditoria(String emailUsuario, String accion, Map<String, Object> detalles) {
        log.info("Guardando log de auditoría para: {}", emailUsuario);
        AuditLog nuevoLog = AuditLog.builder()
                .emailUsuario(emailUsuario)
                .accion(accion)
                .detalles(detalles)
                .fechaHora(java.time.LocalDateTime.now())
                .build();

        auditLogRepository.save(nuevoLog);
    }


}
