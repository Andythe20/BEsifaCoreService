package com.sifa.core_sifa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import com.sifa.core_sifa.dto.infraccion.CoordenadaDTO;
import com.sifa.core_sifa.dto.infraccion.ReporteResumenDTO;
import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.sifa.core_sifa.service.infraccion.IInfraccionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/core/api/v1/infracciones")
@RequiredArgsConstructor
@Slf4j
public class InfraccionController {

    private final IInfraccionService infraccionService;

    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<InfraccionResponse>> getAllInfracciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Obteniendo infracciones paginadas");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("fecha").descending());

        Page<InfraccionResponse> infracciones = infraccionService.findInfracciones(
                startDate,
                endDate,
                user,
                pageable);

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
    public ResponseEntity<Page<InfraccionResponse>> getInfraccionesByIdFiscalizador(
            @PathVariable String idFiscalizador,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Obteniendo infracciones por el id del fiscalizador: {}", idFiscalizador);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("fecha").descending());

        Page<InfraccionResponse> infracciones = infraccionService.findByIdFiscalizador(idFiscalizador, pageable);
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

    // Regla de Negocio: Solo el personal del Juzgado de Policía Local (USER_JPL)
    // puede cambiar el estado de una infracción (Aprobada, Rechazada, etc.)
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PatchMapping("/{id}")
    public ResponseEntity<InfraccionResponse> actualizarEstado(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body,
            @RequestHeader(value = "X-Auth-User", required = false) String idUsuario) {

        String status = body.get("status");
        log.info("Petición PATCH recibida para actualizar estado de infracción ID: {} a {}", id, status);
        InfraccionResponse response = infraccionService.actualizarEstadoInfraccion(id, status, idUsuario);
        return ResponseEntity.ok(response);
    }

    // Regla de Negocio: Solo el personal del Juzgado de Policía Local (USER_JPL)
    // puede editar o corregir campos de la infracción
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}")
    public ResponseEntity<InfraccionResponse> editarInfraccion(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, Object> body) {

        log.info("Petición PUT recibida para editar campos de infracción ID: {}", id);
        InfraccionResponse response = infraccionService.editarInfraccion(id, body);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/coordenadas")
    public ResponseEntity<List<CoordenadaDTO>> getCoordenadas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String user) {

        log.info("Obteniendo coordenadas de infracciones para el mapa de calor");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }

        List<CoordenadaDTO> coordenadas = infraccionService.findCoordenadas(startDate, endDate, user);
        return ResponseEntity.ok(coordenadas);
    }

    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/resumen-reporte")
    public ResponseEntity<ReporteResumenDTO> getResumenReporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String user) {

        log.info("Obteniendo resumen del reporte de infracciones");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }

        ReporteResumenDTO resumen = infraccionService.obtenerResumenReporte(startDate, endDate, user);
        return ResponseEntity.ok(resumen);
    }
}
