package com.example.core_sifa.controller;

import com.example.core_sifa.dto.citacion.CitacionCreateRequest;
import com.example.core_sifa.dto.citacion.CitacionResponse;
import com.example.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.example.core_sifa.service.CitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/api/v1/citaciones")
@RequiredArgsConstructor
@Slf4j
public class CitacionController {

    private final CitacionService citacionService;

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<CitacionResponse> getCitacionById(@PathVariable Integer id) {
        log.info("Petición GET para citación ID: {}", id);

        CitacionResponse response = citacionService.findById(id);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PostMapping
    public ResponseEntity<CitacionResponse> crearCitacion(
            @Valid @RequestBody CitacionCreateRequest request,
            @RequestHeader("X-Auth-User") String emailAdministrativoJpl) {

        log.info("Petición de creación de citación recibida. Administrativo: {}", emailAdministrativoJpl);

        CitacionResponse nuevaCitacion = citacionService.crearCitacion(request, emailAdministrativoJpl);

        // Retornamos 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCitacion);
    }

    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}")
    public ResponseEntity<CitacionResponse> actualizarCitacion(
            @PathVariable Integer id,
            @Valid @RequestBody CitacionUpdateRequest request,
            @RequestHeader("X-Auth-User") String emailAdministrativoJpl) {

        log.info("Petición PUT para actualizar citación ID: {} recibida. Administrativo: {}", id, emailAdministrativoJpl);

        CitacionResponse citacionActualizada = citacionService.actualizarCitacion(id, request, emailAdministrativoJpl);

        return ResponseEntity.ok(citacionActualizada);
    }
}