package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.service.device.IDeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Dispositivos", description = "Registro de tokens FCM para notificaciones push")
public class DeviceTokenController {

    private final IDeviceTokenService deviceTokenService;

    @Operation(summary = "Registrar token FCM del dispositivo",
            description = "Asocia el token FCM al usuario autenticado para enviarle notificaciones push")
    @ApiResponse(responseCode = "200", description = "Token registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content())
    @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_APP')")
    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest request,
            @RequestHeader("X-Auth-User") String emailUsuario) {

        deviceTokenService.register(emailUsuario, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar dispositivos registrados (DTO seguro)",
            description = "Retorna todos los dispositivos registrados sin exponer el token FCM")
    @ApiResponse(responseCode = "200", description = "Lista de dispositivos")
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @GetMapping
    public ResponseEntity<List<DeviceTokenResponse>> listDevices() {
        return ResponseEntity.ok(deviceTokenService.getAllDeviceResponses());
    }

    @Operation(summary = "Estadísticas de dispositivos",
            description = "Retorna conteos de dispositivos por estado")
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getDeviceStats() {
        return ResponseEntity.ok(deviceTokenService.getDeviceStats());
    }
}
