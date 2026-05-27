package com.sifa.core_sifa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sifa.core_sifa.dto.TipoInfraccionDTO;
import com.sifa.core_sifa.service.TipoInfraccionServiceImpl;

@RestController
@RequestMapping("/core/api/v1/tipoInfracciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Catálogo de Tipos de Infracción", description = "Endpoints para la administración y mantención del diccionario de infracciones (CRUD)")
public class TipoInfraccionController {

    private final TipoInfraccionServiceImpl tipoInfraccionService;

    @Operation(
            summary = "Listar catálogo de infracciones paginado",
            description = "Obtiene una lista paginada de todos los tipos de infracción que se encuentran habilitados en el sistema."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo listado correctamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class)))
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR', 'USER_APP')")
    @GetMapping("/all")
    public ResponseEntity<Page<TipoInfraccionDTO>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Obteniendo tipos de infracción paginados - página: {}, tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(tipoInfraccionService.findAllPaged(pageable));
    }

    @Operation(
            summary = "Obtener detalle de un tipo de infracción",
            description = "Busca y retorna los datos de una tipificación específica mediante su ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de infracción encontrado",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TipoInfraccionDTO.class)))
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @ApiResponse(responseCode = "404", description = "El tipo de infracción no existe", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/id/{id}")
    public ResponseEntity<TipoInfraccionDTO> findById(
            @Parameter(description = "ID único del tipo de infracción", required = true) @PathVariable Integer id) {
        TipoInfraccionDTO tipoInfraccion = tipoInfraccionService.findById(id);

        return ResponseEntity.ok(tipoInfraccion);
    }

    // CREATE
    @Operation(
            summary = "Crear nueva tipificación de infracción",
            description = "Agrega una nueva normativa o tipo de infracción al catálogo base. Acción exclusiva para administradores."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de infracción creado y habilitado exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TipoInfraccionDTO.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content())
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @PostMapping
    public ResponseEntity<TipoInfraccionDTO> create(
            @Parameter(description = "Payload con los datos de la infracción", required = true)
            @RequestBody TipoInfraccionDTO tipoInfraccionDTO) {

        log.info("Creando tipo de infracción");

        TipoInfraccionDTO created =
                tipoInfraccionService.create(tipoInfraccionDTO);

        return ResponseEntity.ok(created);
    }

    // UPDATE
    @Operation(
            summary = "Actualizar tipo de infracción",
            description = "Modifica los atributos (nombre, disposición infringida) de un tipo de infracción existente en el catálogo."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro actualizado correctamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TipoInfraccionDTO.class)))
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @ApiResponse(responseCode = "404", description = "El tipo de infracción a modificar no existe", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoInfraccionDTO> update(
            @Parameter(description = "ID del tipo de infracción a editar", required = true) @PathVariable Integer id,
            @Parameter(description = "Payload con los datos actualizados", required = true) @RequestBody TipoInfraccionDTO tipoInfraccionDTO) {

        log.info("Actualizando tipo de infracción con id {}", id);
        TipoInfraccionDTO updated = tipoInfraccionService.update(id, tipoInfraccionDTO);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @Operation(
            summary = "Deshabilitar tipo de infracción (Soft Delete)",
            description = "Realiza un borrado lógico del registro cambiando su estado 'habilitado' a falso. Previene que futuras multas utilicen este código sin romper la integridad referencial de las multas antiguas."
    )
    @ApiResponse(responseCode = "204", description = "Registro deshabilitado exitosamente (Sin contenido de respuesta)", content = @Content())
    @ApiResponse(responseCode = "401", description = "No autorizado, token no proporcionado", content = @Content())
    @ApiResponse(responseCode = "404", description = "El tipo de infracción a eliminar no existe", content = @Content())
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del tipo de infracción a deshabilitar", required = true) @PathVariable Integer id) {

        log.info("Eliminando (deshabilitando) tipo de infracción con id {}", id);
        tipoInfraccionService.delete(id);

        // Retorna 204 No Content que es el estándar estricto de REST para métodos DELETE
        return ResponseEntity.noContent().build();
    }
}
