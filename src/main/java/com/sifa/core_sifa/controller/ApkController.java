package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/core/api/v1/apk")
@RequiredArgsConstructor
@Slf4j
public class ApkController {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final String ALLOWED_EXTENSION = ".apk";
    private static final String ALLOWED_MIME = "application/vnd.android.package-archive";
    private static final String FALLBACK_MIME = "application/octet-stream";
    private static final byte[] ZIP_MAGIC = { 0x50, 0x4B };

    private final IStorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadApk(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : "unknown";

        // 1. No vacío
        if (file == null || file.isEmpty()) {
            log.warn("Intento de subida sin archivo por {}", email);
            return ResponseEntity.badRequest().body(Map.of("error", "Debes seleccionar un archivo APK"));
        }

        // 2. Validar extensión
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
            log.warn("Extensión inválida: {} por {}", originalName, email);
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos con extensión .apk"));
        }

        // 3. Sanitizar nombre (evitar path traversal)
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!sanitized.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre de archivo inválido"));
        }

        // 4. Validar content-type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals(ALLOWED_MIME) && !contentType.equals(FALLBACK_MIME))) {
            log.warn("Content-Type inválido: {} por {}", contentType, email);
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de archivo no válido"));
        }

        // 5. Validar tamaño máximo
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Archivo demasiado grande: {} bytes por {}", file.getSize(), email);
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no puede superar los 50MB"));
        }

        // 6. Validar magic bytes (PK zip header)
        try {
            byte[] header = new byte[2];
            file.getInputStream().read(header, 0, 2);
            if (!Arrays.equals(header, ZIP_MAGIC)) {
                log.warn("Magic bytes inválidos por {}", email);
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo no tiene un formato APK válido"));
            }
        } catch (Exception e) {
            log.error("Error leyendo magic bytes", e);
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo validar el archivo"));
        }

        try {
            String url = storageService.uploadApk(file);

            log.info("APK subido exitosamente por {} -> {}", email, url);

return ResponseEntity.ok(Map.of(
        "message", "Nueva versión de la aplicación publicada correctamente"
));

        } catch (Exception e) {
            log.error("Error subiendo APK por {}", email, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al subir el APK: " + e.getMessage()));
        }
    }
}
