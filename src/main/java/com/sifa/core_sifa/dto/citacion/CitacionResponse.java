package com.sifa.core_sifa.dto.citacion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.model.Citacion;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionResponse {

    @Schema(description = "Identificador de la citacion", example = "1")
    private Integer idCitacion;
    @Schema(description = "Fecha de la citacion", example = "2026-05-26T15:19:36")
    private LocalDateTime fecha;
    @Schema(description = "Datos completos de la infracción asociada a esta citación")
    private InfraccionResponse infraccion;

    public static CitacionResponse fromEntity(Citacion citacion) {
        return CitacionResponse.builder()
                .idCitacion(citacion.getIdCitacion())
                .fecha(citacion.getFecha())
                .infraccion(InfraccionResponse.fromEntity(citacion.getInfraccion()))
                .build();
    }

}
