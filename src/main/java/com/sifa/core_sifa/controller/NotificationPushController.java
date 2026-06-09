package com.sifa.core_sifa.controller;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.dto.push.BulkPushRequest;
import com.sifa.core_sifa.dto.push.OutdatedPushRequest;
import com.sifa.core_sifa.dto.push.SelectPushRequest;
import com.sifa.core_sifa.dto.push.SinglePushRequest;
import com.sifa.core_sifa.service.device.IDeviceTokenService;
import com.sifa.core_sifa.service.notification.INotificationLogService;
import com.sifa.core_sifa.service.push.IPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/core/api/v1/notifications/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications (Test)", description = "Endpoints para probar el envío de notificaciones push")
public class NotificationPushController {

    private final IPushService pushService;
    private final IDeviceTokenService deviceTokenService;
    private final INotificationLogService notificationLogService;

    @Operation(summary = "Enviar notificación push de prueba",
            description = "Envía una notificación push a un dispositivo específico usando su token FCM")
    @ApiResponse(responseCode = "200", description = "Notificación enviada exitosamente")
    @ApiResponse(responseCode = "400", description = "Error al enviar", content = @Content())
    @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_SUPERVISOR', 'USER_ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, String>> sendPush(
            @Valid @RequestBody SinglePushRequest request,
            @RequestHeader("X-Auth-User") String user) {

        try {
            String messageId = pushService.send(request.getToken(), request.getTitle(), request.getBody());
            notificationLogService.log("SINGLE", request.getTitle(), request.getBody(),
                    null, null, 1, user);
            return ResponseEntity.ok(Map.of("messageId", messageId));
        } catch (FirebaseMessagingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "code", String.valueOf(e.getErrorCode()),
                    "cause", e.getCause() != null ? e.getCause().getMessage() : "none"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }

    @Operation(summary = "Diagnóstico de conectividad Firebase",
            description = "Verifica conectividad con Google OAuth2 y estado de Firebase")
    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_SUPERVISOR', 'USER_ADMIN')")
    @GetMapping("/diagnostico")
    public ResponseEntity<Map<String, Object>> diagnostico() {
        try {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("firebase_inicializado", !FirebaseApp.getApps().isEmpty());

            InetAddress oauth2 = InetAddress.getByName("oauth2.googleapis.com");
            result.put("dns_oauth2", oauth2.getHostAddress());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            long start = System.currentTimeMillis();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;

            result.put("oauth2_status", resp.statusCode());
            result.put("oauth2_ms", elapsed);
            result.put("oauth2_body_length", resp.body().length());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage()
            ));
        }
    }

    @Operation(summary = "Notificar a todos los dispositivos",
            description = "Envía una notificación push a todos los dispositivos registrados")
    @ApiResponse(responseCode = "200", description = "Notificaciones enviadas")
    @ApiResponse(responseCode = "400", description = "Error", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> notifyAll(
            @Valid @RequestBody BulkPushRequest request,
            @RequestHeader("X-Auth-User") String user) {
        try {
            int sent = deviceTokenService.notifyAllDevices(
                    request.getTitle(),
                    request.getBody()
            );
            notificationLogService.log("ALL", request.getTitle(), request.getBody(),
                    null, null, sent, user);
            return ResponseEntity.ok(Map.of("sent", sent));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Notificar por plataforma",
            description = "Envía una notificación push a todos los dispositivos de una plataforma específica")
    @ApiResponse(responseCode = "200", description = "Notificaciones enviadas")
    @ApiResponse(responseCode = "400", description = "Error", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @PostMapping("/platform")
    public ResponseEntity<Map<String, Object>> notifyByPlatform(
            @Valid @RequestBody BulkPushRequest request,
            @RequestHeader("X-Auth-User") String user) {
        try {
            int sent = deviceTokenService.notifyByPlatform(
                    request.getTargetPlatform(),
                    request.getTitle(),
                    request.getBody()
            );
            notificationLogService.log("PLATFORM", request.getTitle(), request.getBody(),
                    null, request.getTargetPlatform(), sent, user);
            return ResponseEntity.ok(Map.of(
                    "sent", sent,
                    "platform", request.getTargetPlatform()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Listar dispositivos registrados (DTO seguro)",
            description = "Retorna todos los dispositivos registrados sin exponer el token FCM")
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceTokenResponse>> listDevices() {
        return ResponseEntity.ok(deviceTokenService.getAllDeviceResponses());
    }

    @Operation(summary = "Notificar a dispositivos seleccionados",
            description = "Envía una notificación push a dispositivos específicos por ID")
    @ApiResponse(responseCode = "200", description = "Notificaciones enviadas")
    @ApiResponse(responseCode = "400", description = "Error", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @PostMapping("/select")
    public ResponseEntity<Map<String, Object>> notifySelect(
            @Valid @RequestBody SelectPushRequest request,
            @RequestHeader("X-Auth-User") String user) {
        try {
            int sent = deviceTokenService.notifyDevicesByIds(
                    request.getDeviceIds(),
                    request.getTitle(),
                    request.getBody()
            );
            notificationLogService.log("SELECT", request.getTitle(), request.getBody(),
                    null, null, sent, user);
            return ResponseEntity.ok(Map.of("sent", sent));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Notificar dispositivos desactualizados",
            description = "Envía una notificación push a todos los dispositivos cuya versión no coincida con la versión actual")
    @ApiResponse(responseCode = "200", description = "Notificaciones enviadas")
    @ApiResponse(responseCode = "400", description = "Error", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @PostMapping("/outdated")
    public ResponseEntity<Map<String, Object>> notifyOutdated(
            @Valid @RequestBody OutdatedPushRequest request,
            @RequestHeader("X-Auth-User") String user) {
        try {
            int sent = deviceTokenService.notifyOutdatedDevices(
                    request.getCurrentVersion(),
                    request.getTitle(),
                    request.getBody()
            );
            notificationLogService.log("OUTDATED", request.getTitle(), request.getBody(),
                    request.getCurrentVersion(), null, sent, user);
            return ResponseEntity.ok(Map.of(
                    "sent", sent,
                    "currentVersion", request.getCurrentVersion()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
