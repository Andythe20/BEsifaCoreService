package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.dto.backup.BackupJobResponse;
import com.sifa.core_sifa.dto.backup.BackupListResponse;
import com.sifa.core_sifa.dto.backup.DownloadResponse;
import com.sifa.core_sifa.service.backup.IBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

@RestController
@RequestMapping("/core/api/v1/admin/backups")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Backups", description = "Administración de respaldos y restauraciones")
public class BackupController {

    private final IBackupService backupService;

    @Operation(summary = "Crear backup completo")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @PostMapping("/full")
    public ResponseEntity<BackupJobResponse> createFullBackup(Authentication auth) {
        log.info("Solicitado backup completo");
        String username = auth != null ? auth.getName() : null;
        BackupJobResponse response = backupService.createFullBackup(username);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Listar backups")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<BackupListResponse>> listBackups() {
        log.info("Listando backups");
        return ResponseEntity.ok(backupService.listBackups());
    }

    @Operation(summary = "Obtener estado de un job")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BackupJobResponse> getJobStatus(@PathVariable @NotBlank String jobId) {
        return ResponseEntity.ok(backupService.getJobStatus(jobId));
    }

    @Operation(summary = "Descargar backup")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @GetMapping("/{backupId}/download")
    public ResponseEntity<DownloadResponse> downloadBackup(@PathVariable @NotBlank String backupId) {
        return ResponseEntity.ok(backupService.downloadBackup(backupId));
    }

    @Operation(summary = "Eliminar backup")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @DeleteMapping("/{backupId}")
    public ResponseEntity<Void> deleteBackup(@PathVariable @NotBlank String backupId) {
        log.info("Solicitada eliminación de backup {}", backupId);
        backupService.deleteBackup(backupId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir archivo ZIP como nuevo backup (sin restaurar)")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @PostMapping(value = "/upload-backup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupJobResponse> uploadBackup(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Solicitada subida de backup: {} ({} bytes)", name, file.getSize());

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("upload-backup-", ".zip");
            file.transferTo(tempZip.toFile());
            String username = auth != null ? auth.getName() : null;
            BackupJobResponse response = backupService.uploadBackup(tempZip, username, description);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            if (tempZip != null) {
                try { Files.deleteIfExists(tempZip); } catch (Exception ignored) {}
            }
            log.error("Error al procesar archivo subido: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Restaurar desde archivo subido")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    @PostMapping(value = "/upload-restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupJobResponse> uploadRestore(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Solicitada restauración desde archivo: {} ({} bytes)", name, file.getSize());

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("upload-restore-", ".zip");
            file.transferTo(tempZip.toFile());
            BackupJobResponse response = backupService.uploadRestore(tempZip);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            if (tempZip != null) {
                try { Files.deleteIfExists(tempZip); } catch (Exception ignored) {}
            }
            log.error("Error al procesar archivo subido: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }
}
