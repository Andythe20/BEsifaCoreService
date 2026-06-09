package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.dto.audit.AuditLogResponseDTO;
import com.sifa.core_sifa.service.audits.AuditLogServiceImpl;
import com.sifa.core_sifa.service.audits.IAuditLogService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Hidden // Oculta este controlador de la documentación de Swagger ya que es de uso interno
@RestController
@RequestMapping("/core/api/v1/internal/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final IAuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Void> crearLogInterno(@RequestBody AuditLogRequestDTO request) {
        auditLogService.registrarLog(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDTO>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Obteniendo auditorias paginadas");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }

        Page<AuditLogResponseDTO> logs = auditLogService.findAll(
                startDate,
                endDate,
                user,
                search,
                pageable
        );
        return ResponseEntity.ok(logs);

    }
}
