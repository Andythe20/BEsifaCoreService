package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.dto.backup.RestoreJobResponse;
import com.sifa.core_sifa.dto.backup.RestoreRequest;
import com.sifa.core_sifa.dto.backup.RestoreValidationResponse;
import com.sifa.core_sifa.service.backup.IRestoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/api/v1/admin/restore")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restore", description = "Sistema de restauración segura con validación, schemas temporales y swap atómico")
public class RestoreController {

    private final IRestoreService restoreService;

    @Operation(summary = "Validar backup sin restaurar (dry-run)")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @GetMapping("/validate/{backupId}")
    public ResponseEntity<RestoreValidationResponse> validateBackup(@PathVariable @NotBlank String backupId) {
        log.info("Validación solicitada para backup {}", backupId);
        RestoreValidationResponse response = restoreService.validateBackup(backupId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Iniciar restauración segura")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @PostMapping("/{backupId}")
    public ResponseEntity<RestoreJobResponse> startRestore(@PathVariable @NotBlank String backupId,
                                                            @RequestBody(required = false) RestoreRequest request) {
        String scope = (request != null && request.getScope() != null) ? request.getScope() : "full";
        log.info("Restore {} solicitado para backup {} (scope: {})", scope, backupId);
        RestoreJobResponse response = restoreService.startRestore(backupId, scope);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Obtener estado del restore")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<RestoreJobResponse> getRestoreStatus(@PathVariable @NotBlank String jobId) {
        return ResponseEntity.ok(restoreService.getRestoreStatus(jobId));
    }

    @Operation(summary = "Cancelar restore en progreso")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelRestore(@PathVariable @NotBlank String jobId) {
        log.info("Cancelación solicitada para restore job {}", jobId);
        restoreService.cancelRestore(jobId);
        return ResponseEntity.ok().build();
    }
}
