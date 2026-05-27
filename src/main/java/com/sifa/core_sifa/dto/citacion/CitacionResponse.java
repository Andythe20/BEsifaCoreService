package com.sifa.core_sifa.dto.citacion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

import com.sifa.core_sifa.model.Citacion;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionResponse {

    @Schema(description = "Identificador de la citacion", example = "1")
    private Integer idCitacion;
    @Schema(description = "Fecha de la citacion", example = "2026-05-26 15:19:36.828831")
    private LocalDateTime fecha;
    @Schema(description = "Identificador de la infracción", example = "1")
    private Integer idInfraccion;

    public static CitacionResponse fromEntity(Citacion citacion) {
        return CitacionResponse.builder()
                .idCitacion(citacion.getIdCitacion())
                .fecha(citacion.getFecha())
                .idInfraccion(citacion.getInfraccion().getIdInfraccion())
                .build();
    }

}
