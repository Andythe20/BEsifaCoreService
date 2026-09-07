package com.sifa.core_sifa.service.audits;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.util.ChecksumUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private final IAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // Formato fijo de 6 dígitos (microsegundos): coincide con la precisión
    // de DATETIME(6) en MySQL y mantiene el hash estable en el round-trip BD <-> JVM.
    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

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
    public synchronized void registrarLog(AuditLogRequestDTO request) {
        log.debug("[AUDITORIA] Registrando log -> Usuario: {} | Acción: {}", request.getEmailUsuario(), request.getAccion());

        // Encadena con el hash del último log para detectar inserciones/borrados en el historial
        String hashAnterior = obtenerUltimoHash();

        AuditLog logEntity = AuditLog.builder()
                .emailUsuario(request.getEmailUsuario())
                .accion(request.getAccion())
                .tablaAfectada(request.getTablaAfectada())
                .idRegistroAfectado(request.getIdRegistroAfectado())
                .detalles(request.getDetalles())
                .hashAnterior(hashAnterior)
                .build();
        // Timestamp definitivo, truncado a microsegundos: es el valor que se hashea
        // y el que se persiste (el @PrePersist ya no lo sobrescribe)
        logEntity.setFechaHora(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        logEntity.setHashActual(calcularHash(logEntity));

        // La tabla de auditoría es de solo inserción: nunca se actualiza ni elimina un log.
        auditLogRepository.save(logEntity);
        log.info("Auditoría guardada exitosamente | Usuario: {} | Acción: {}", request.getEmailUsuario(), request.getAccion());
    }

    /**
     * Verifica la cadena de hashes de auditoría recorriendo los logs en orden.
     * Devuelve el resultado indicando si la cadena es íntegra y qué eventos la rompen.
     */
    @Override
    @Transactional(readOnly = true)
    public CadenaVerificacion verificarCadena() {
        List<AuditLog> logs = auditLogRepository.findAllOrderByIdAsc();
        List<String> errores = new java.util.ArrayList<>();

        for (int i = 0; i < logs.size(); i++) {
            AuditLog log = logs.get(i);

            // 1. Verificar que el hashAnterior apunte al hash del log previo
            String prev = (i == 0) ? null : logs.get(i - 1).getHashActual();
            if (!java.util.Objects.equals(prev, log.getHashAnterior())) {
                errores.add("Evento #" + log.getIdAuditLog() + " (accion=" + log.getAccion() + "): hashAnterior incorrecto. Se esperaba " + prev + " pero se encontró " + log.getHashAnterior());
            }

            // 2. Verificar que el hashActual del log coincida con el hash calculado sobre su contenido
            String calculado = calcularHash(log);
            if (!calculado.equals(log.getHashActual())) {
                errores.add("Evento #" + log.getIdAuditLog() + " (accion=" + log.getAccion() + "): hashActual alterado o contenido modificado");
            }
        }

        return new CadenaVerificacion(logs.size(), errores.isEmpty(), errores, logs);
    }

    private String obtenerUltimoHash() {
        List<AuditLog> ultimos = auditLogRepository.findTopByOrderByIdAuditLogDesc(PageRequest.of(0, 1));
        if (ultimos.isEmpty() || ultimos.get(0).getHashActual() == null) {
            return null;
        }
        return ultimos.get(0).getHashActual();
    }

    /**
     * Calcula el hash SHA-256 del contenido del log. Para que sea determinista,
     * se serializan los detalles de forma ordenada (TreeMap/JSON canónico).
     */
    String calcularHash(AuditLog log) {
        String detallesJson;
        if (log.getDetalles() == null) {
            detallesJson = "null";
        } else {
            try {
                detallesJson = objectMapper.writeValueAsString(new java.util.TreeMap<>(log.getDetalles()));
            } catch (JsonProcessingException e) {
                // Fallback: representación textual estable de los detalles
                detallesJson = new java.util.TreeMap<>(log.getDetalles()).toString();
            }
        }

        String contenido = String.join("|",
                log.getEmailUsuario() != null ? log.getEmailUsuario() : "",
                log.getAccion() != null ? log.getAccion() : "",
                log.getTablaAfectada() != null ? log.getTablaAfectada() : "",
                log.getIdRegistroAfectado() != null ? log.getIdRegistroAfectado() : "",
                detallesJson,
                log.getFechaHora() != null ? log.getFechaHora().format(AUDIT_TIMESTAMP_FORMAT) : "",
                log.getHashAnterior() != null ? log.getHashAnterior() : ""
        );

        return ChecksumUtil.sha256(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Resultado de la verificación de la cadena de auditoría.
     */
    public record CadenaVerificacion(
            int totalEventos,
            boolean integra,
            List<String> errores,
            List<AuditLog> logs
    ) {
    }

}
