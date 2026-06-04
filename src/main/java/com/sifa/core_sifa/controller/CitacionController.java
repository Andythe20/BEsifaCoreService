package com.sifa.core_sifa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sifa.core_sifa.dto.citacion.CitacionResponse;
import com.sifa.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.sifa.core_sifa.service.CitacionService;

import java.time.LocalDate;

@RestController
@RequestMapping("/core/api/v1/citaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Citaciones JPL", description = "Endpoints para el agendamiento, reprogramación y consulta de citaciones al Juzgado de Policía Local")
public class CitacionController {

    private final CitacionService citacionService;

    @Operation(
            summary = "Listar citaciones con filtros",
            description = "Retorna un listado paginado de citaciones con sus infracciones asociadas. Soporta filtrado por rango de fechas y búsqueda por patente, RUT o nombre del infractor."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de citaciones obtenido exitosamente")
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<CitacionResponse>> getAllCitaciones(
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Fecha inicio del rango (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha fin del rango (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Búsqueda por patente, RUT, nombre o ID infracción")
            @RequestParam(required = false) String search) {

        log.info("Petición GET para listar citaciones - page: {}, size: {}, startDate: {}, endDate: {}, search: {}",
                page, size, startDate, endDate, search);

        Pageable pageable = PageRequest.of(page, size);
        Page<CitacionResponse> citaciones = citacionService.findAll(startDate, endDate, search, pageable);

        return ResponseEntity.ok(citaciones);
    }

    @Operation(
            summary = "Obtener detalle de una citación",
            description = "Busca y retorna los datos de agendamiento de una citación específica mediante su identificador único."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Citación encontrada exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CitacionResponse.class)))
    @ApiResponse(responseCode = "404", description = "La citación solicitada no existe", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<CitacionResponse> getCitacionById(
            @Parameter(description = "ID único de la citación", required = true) @PathVariable Integer id) {

        log.info("Petición GET para citación ID: {}", id);
        CitacionResponse response = citacionService.findById(id);

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Reprogramar citación",
            description = "Actualiza la fecha de una citación existente. Se bloquea la acción si la citación original ya ocurrió en el pasado o si la nueva fecha propuesta es anterior a la fecha actual."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Citación reprogramada correctamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CitacionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validación de fechas fallida (intentando agendar en el pasado)", content = @Content())
    @ApiResponse(responseCode = "404", description = "La citación a modificar no existe", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}")
    public ResponseEntity<CitacionResponse> actualizarCitacion(
            @Parameter(description = "ID de la citación a reprogramar", required = true) @PathVariable Integer id,

            @Parameter(description = "Payload con la nueva fecha y hora para la cita", required = true)
            @Valid @RequestBody CitacionUpdateRequest request,

            @Parameter(hidden = true) @RequestHeader("X-Auth-User") String emailAdministrativoJpl) {

        log.info("Petición PUT para actualizar citación ID: {} recibida. Administrativo: {}", id, emailAdministrativoJpl);
        CitacionResponse citacionActualizada = citacionService.actualizarCitacion(id, request, emailAdministrativoJpl);

        return ResponseEntity.ok(citacionActualizada);
    }
}