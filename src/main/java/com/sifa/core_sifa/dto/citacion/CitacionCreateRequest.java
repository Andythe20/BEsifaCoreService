package com.sifa.core_sifa.dto.citacion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Este DTO sólo será instanciado cuando llegue el json para crear la citacion.
 * No se requiere construirlo a partir de una entidad de la bbdd.
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionCreateRequest {
    @Schema(description = "Fecha de la citacion", example = "2026-05-26 15:19:36.828831")
    private LocalDateTime fecha;
    @Schema(description = "Identificador de la infracción", example = "1")
    private Integer idInfraccion;

}
