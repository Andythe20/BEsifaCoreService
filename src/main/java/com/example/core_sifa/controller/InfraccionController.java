package com.example.core_sifa.controller;

import com.example.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.example.core_sifa.dto.infraccion.InfraccionResponse;
import com.example.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.example.core_sifa.service.InfraccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/core/api/v1/infracciones")
@RequiredArgsConstructor
@Slf4j
public class InfraccionController {

    private final InfraccionService infraccionService;

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/all")
    public ResponseEntity<List<InfraccionResponse>> getAllInfracciones(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        List<InfraccionResponse> infracciones;

        if (date != null) {
            log.info("Filtrando infracciones por fecha: {}", date);
            infracciones = infraccionService.findByDate(date);
        } else {
            log.info("Obteniendo todas las infracciones");
            infracciones = infraccionService.findAllInfracciones();
        }

        if (infracciones.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(infracciones);
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/id/{id}")
    public ResponseEntity<InfraccionResponse> getInfraccionById(@PathVariable Integer id) {
        log.info("Obteniendo infraccion con id: {}", id);
        InfraccionResponse infraccion = infraccionService.findById(id);
        return ResponseEntity.ok(infraccion);
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/fiscalizador/{idFiscalizador}")
    public ResponseEntity<List<InfraccionResponse>> getInfraccionesByIdFiscalizador(
            @PathVariable String idFiscalizador) {
        log.info("Obteniendo infracciones por el id del fiscalizador: {}", idFiscalizador);
        List<InfraccionResponse> infracciones = infraccionService.findByIdFiscalizador(idFiscalizador);
        return ResponseEntity.ok(infracciones);
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/vehiculo/{vehiculoPatente}")
    public ResponseEntity<List<InfraccionResponse>> getInfraccionesByVehiculoPatente(
            @PathVariable String vehiculoPatente) {
        log.info("Obteniendo infracciones por la patente: {}", vehiculoPatente);
        List<InfraccionResponse> infracciones = infraccionService.findByVehiculoPatente(vehiculoPatente);
        return ResponseEntity.ok(infracciones);
    }

    // Se usa el @RequestPart para recibir el JSON con los datos y los archivos
    @PreAuthorize("hasAnyAuthority('USER_APP')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InfraccionResponse> crearInfraccion(
            @RequestPart("infraccion") @Valid InfraccionCreateRequest request,
            @RequestPart("fotos") List<MultipartFile> fotos,
            @RequestHeader("X-Auth-User") String idFiscalizador) {

        log.info("Petición de creación de infracción recibida desde el Gateway. Fiscalizador ID: {}", idFiscalizador);

        InfraccionResponse nuevaInfraccion = infraccionService.crearInfraccion(request, fotos, idFiscalizador);

        // Devolvemos un 201 CREATED, que es el estándar REST para recursos nuevos
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaInfraccion);
    }

    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}/procesar")
    public ResponseEntity<InfraccionResponse> procesarInfraccion(
            @PathVariable Integer id,
            @Valid @RequestBody InfraccionUpdateRequest request,
            @RequestHeader("X-Auth-User") String idAdministrativoJpl) {

        log.info("Petición para procesar infracción ID: {} recibida. Administrativo JPL ID: {}", id,
                idAdministrativoJpl);

        InfraccionResponse infraccionActualizada = infraccionService.procesarInfraccionPorJpl(id, request,
                idAdministrativoJpl);

        // Devolvemos un 200 OK con el recurso actualizado
        return ResponseEntity.ok(infraccionActualizada);
    }
}
