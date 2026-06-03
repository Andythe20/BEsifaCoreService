package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.service.AuditLogService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden // Oculta este controlador de la documentación de Swagger ya que es de uso interno
@RestController
@RequestMapping("/internal/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Void> crearLogInterno(@RequestBody AuditLogRequestDTO request) {
        auditLogService.registrarLog(request);
        return ResponseEntity.ok().build();
    }
}
