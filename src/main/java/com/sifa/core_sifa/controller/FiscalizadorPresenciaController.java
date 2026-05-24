package com.sifa.core_sifa.controller;


import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.service.FiscalizadorPresenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core/api/v1/fis-activity")
@RequiredArgsConstructor
public class FiscalizadorPresenciaController {

    private final FiscalizadorPresenciaService presenciaService;

    /**
     * Endpoint que llamará la App Móvil periódicamente.
     * Rol requerido: USER_APP (Fiscalizador)
     */
    @PreAuthorize("hasAnyAuthority('USER_APP')")
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> recibirHeartbeat(
            @Valid @RequestBody FiscalizadorHeartbeatRequest request,
            @RequestHeader("X-Auth-User") String emailInspector) {

        presenciaService.registrarLatido(emailInspector, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint que consultará el Dashboard Web del supervisor.
     * Roles permitidos: Supervisores o Administradores
     */
    @PreAuthorize("hasAnyAuthority('USER_SUPERVISOR', 'USER_ADMIN')")
    @GetMapping("/activos")
    public ResponseEntity<List<FiscalizadorPresencia>> getFiscalizadoresActivos() {
        List<FiscalizadorPresencia> activos = presenciaService.obtenerFiscalizadoresActivos();
        return ResponseEntity.ok(activos);
    }
}
