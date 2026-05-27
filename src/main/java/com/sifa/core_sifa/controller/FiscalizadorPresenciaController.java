package com.sifa.core_sifa.controller;


import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.service.FiscalizadorPresenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/api/v1/fis-activity")
@RequiredArgsConstructor
@Tag(name = "Telemetría y Presencia", description = "Endpoints para el rastreo GPS y monitoreo de actividad de fiscalizadores en terreno")
public class FiscalizadorPresenciaController {

    private final FiscalizadorPresenciaService presenciaService;

    /**
     * Endpoint que llamará la App Móvil periódicamente.
     * Rol requerido: USER_APP (Fiscalizador)
     */
    @Operation(
            summary = "Registrar latido GPS (Heartbeat)",
            description = "Invocado periódicamente en segundo plano por la App Móvil para actualizar la ubicación geográfica en tiempo real del fiscalizador. Mantiene su estado como 'Activo' en el Dashboard."
    )
    @ApiResponse(responseCode = "200", description = "Latido de presencia registrado y actualizado exitosamente")
    @ApiResponse(responseCode = "400", description = "El payload de coordenadas es inválido o está vacío", content = @Content())
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_APP')")
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> recibirHeartbeat(
            @Parameter(description = "Payload con las coordenadas GPS actuales", required = true)
            @Valid @RequestBody FiscalizadorHeartbeatRequest request,

            @Parameter(hidden = true) @RequestHeader("X-Auth-User") String emailInspector) {

        presenciaService.registrarLatido(emailInspector, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint que consultará el Dashboard Web del supervisor.
     * Roles permitidos: Supervisores o Administradores
     */
    @Operation(
            summary = "Listar fiscalizadores activos",
            description = "Retorna una lista paginada con la última ubicación conocida de los fiscalizadores. Por regla de negocio, solo incluye a aquellos que han emitido un latido (heartbeat) en los últimos 10 minutos."
    )
    @ApiResponse(responseCode = "200", description = "Lista de fiscalizadores activos obtenida correctamente")
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_SUPERVISOR', 'USER_ADMIN')")
    @GetMapping("/activos")
    public ResponseEntity<Page<FiscalizadorPresencia>> getFiscalizadoresActivos(
            @ParameterObject @PageableDefault(size = 10, sort = "ultimaConexion", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(presenciaService.obtenerFiscalizadoresActivos(pageable));
    }
}
