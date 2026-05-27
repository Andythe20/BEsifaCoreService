package com.sifa.core_sifa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.sifa.core_sifa.service.infraccion.IInfraccionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/core/api/v1/infracciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestión de Infracciones", description = "Endpoints para la creación, edición y seguimiento procesal de las infracciones de tránsito")
public class InfraccionController {

    private final IInfraccionService infraccionService;

    @Operation(summary = "Obtener todas las infracciones paginadas", description = "Permite buscar y listar infracciones con filtros opcionales de fecha y usuario.")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de infracciones obtenida exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Parámetros de solicitud inválidos", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_APP', 'USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<InfraccionResponse>> getAllInfracciones(
            @Parameter(description = "Fecha de inicio (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filtro por ID Fiscalizador (Email)") @RequestParam(required = false) String user,
            @ParameterObject @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Obteniendo infracciones paginadas");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }

        Page<InfraccionResponse> infracciones = infraccionService.findInfracciones(
                startDate,
                endDate,
                user,
                pageable);

        return ResponseEntity.ok(infracciones);
    }

    @Operation(summary = "Obtener infracción por ID", description = "Busca una infracción específica utilizando su identificador único.")
    @ApiResponse(
            responseCode = "200",
            description = "Infracción encontrada",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Infracción no encontrada", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/id/{id}")
    public ResponseEntity<InfraccionResponse> getInfraccionById(
            @Parameter(description = "ID numérico de la infracción", required = true) @PathVariable Integer id) {
        log.info("Obteniendo infraccion con id: {}", id);
        InfraccionResponse infraccion = infraccionService.findById(id);
        return ResponseEntity.ok(infraccion);
    }

    @Operation(summary = "Obtener infracciones por Fiscalizador", description = "Lista el historial paginado de infracciones cursadas por un fiscalizador específico.")
    @ApiResponse(
            responseCode = "200",
            description = "Historial obtenido",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Fiscalizador no encontrado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/fiscalizador/{idFiscalizador}")
    public ResponseEntity<Page<InfraccionResponse>> getInfraccionesByIdFiscalizador(
            @Parameter(description = "Email del fiscalizador", required = true) @PathVariable String idFiscalizador,
            @ParameterObject @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        log.info("Obteniendo infracciones por el id del fiscalizador: {}", idFiscalizador);

        Page<InfraccionResponse> infracciones = infraccionService.findByIdFiscalizador(idFiscalizador, pageable);
        return ResponseEntity.ok(infracciones);
    }

    @Operation(summary = "Obtener infracciones por Patente", description = "Devuelve el listado completo de infracciones asociadas a un vehículo específico.")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de infracciones del vehículo",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Patente no encontrada", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_SUPERVISOR')")
    @GetMapping("/vehiculo/{vehiculoPatente}")
    public ResponseEntity<List<InfraccionResponse>> getInfraccionesByVehiculoPatente(
            @Parameter(description = "Patente del vehículo (Ej: BBCC11)", required = true) @PathVariable String vehiculoPatente) {
        log.info("Obteniendo infracciones por la patente: {}", vehiculoPatente);
        List<InfraccionResponse> infracciones = infraccionService.findByVehiculoPatente(vehiculoPatente);
        return ResponseEntity.ok(infracciones);
    }

    // Se usa el @RequestPart para recibir el JSON con los datos y los archivos
    @Operation(summary = "Crear nueva infracción (Multipart)", description = "Sube el JSON estructurado de la multa y el arreglo de fotografías físicas capturadas en terreno.")
    @ApiResponse(
            responseCode = "201",
            description = "Infracción tipificada y guardada con éxito",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validación fallida en los datos", content = @Content())
    @ApiResponse(responseCode = "415", description = "Falta el header multipart/form-data", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_APP')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InfraccionResponse> crearInfraccion(
            @Parameter(description = "JSON de la Infracción (Objeto InfraccionCreateRequest)", required = true)
            @RequestPart("infraccion") @Valid InfraccionCreateRequest request,

            // Forzamos a Swagger a mostrar el botón nativo de subida de múltiples archivos
            @Parameter(
                    description = "Archivos de imagen (Fotografías de evidencia)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @RequestPart("fotos") List<MultipartFile> fotos,

            @Parameter(hidden = true) @RequestHeader("X-Auth-User") String idFiscalizador) {

        log.info("Petición de creación de infracción recibida desde el Gateway. Fiscalizador ID: {}", idFiscalizador);

        InfraccionResponse nuevaInfraccion = infraccionService.crearInfraccion(request, fotos, idFiscalizador);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaInfraccion);
    }

    @Operation(summary = "Procesar infracción formalmente", description = "Agrega motivos de rechazo o procesa definitivamente una multa en el JPL.")
    @ApiResponse(
            responseCode = "200",
            description = "Infracción procesada con éxito",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Infracción no encontrada", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}/procesar")
    public ResponseEntity<InfraccionResponse> procesarInfraccion(
            @Parameter(description = "ID de la infracción") @PathVariable Integer id,
            @Valid @RequestBody InfraccionUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader("X-Auth-User") String idAdministrativoJpl) {

        log.info("Petición para procesar infracción ID: {} recibida. Administrativo JPL ID: {}", id, idAdministrativoJpl);

        InfraccionResponse infraccionActualizada = infraccionService.procesarInfraccionPorJpl(id, request, idAdministrativoJpl);

        return ResponseEntity.ok(infraccionActualizada);
    }

    @Operation(summary = "Actualizar estado rápido de infracción", description = "Modifica el estado transaccional (ej: pending, accepted, rejected).")
    @ApiResponse(
            responseCode = "200",
            description = "Estado modificado exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Infracción no encontrada", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PatchMapping("/{id}")
    public ResponseEntity<InfraccionResponse> actualizarEstado(
            @Parameter(description = "ID de la infracción", required = true) @PathVariable Integer id,

            // Usamos RequestBody nativo de Swagger para inyectar un JSON de ejemplo y borrar 'additionalProp1'
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cuerpo JSON con el estado. Valores típicos: pending, accepted, rejected, exported.",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n  \"status\": \"accepted\"\n}"))
            )
            @RequestBody Map<String, String> body,

            @Parameter(hidden = true) @RequestHeader(value = "X-Auth-User", required = false) String idUsuario) {

        String status = body.get("status");
        log.info("Petición PATCH recibida para actualizar estado de infracción ID: {} a {}", id, status);
        InfraccionResponse response = infraccionService.actualizarEstadoInfraccion(id, status, idUsuario);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Edición avanzada de campos", description = "Permite a un funcionario modificar selectivamente cualquier campo permitido de la multa.")
    @ApiResponse(
            responseCode = "200",
            description = "Infracción modificada exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InfraccionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Infracción no encontrada", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_JPL')")
    @PutMapping("/{id}")
    public ResponseEntity<InfraccionResponse> editarInfraccion(
            @Parameter(description = "ID de la infracción", required = true) @PathVariable Integer id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cuerpo JSON con los atributos dinámicos a modificar",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n  \"observaciones\": \"Se corrigió el detalle del parte\",\n  \"infractionCode\": 3\n}"))
            )
            @RequestBody Map<String, Object> body) {

        log.info("Petición PUT recibida para editar campos de infracción ID: {}", id);
        InfraccionResponse response = infraccionService.editarInfraccion(id, body);
        return ResponseEntity.ok(response);
    }
}
