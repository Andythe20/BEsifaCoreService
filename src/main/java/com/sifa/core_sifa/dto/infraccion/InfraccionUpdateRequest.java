package com.sifa.core_sifa.dto.infraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraccionUpdateRequest {

    @Schema(description = "Estado de la infraccion, depende de como lo procese el JPL", example = "ACEPTADO")
    private String estado;
    @Schema(description = "En caso de ser rechazada, se llena este campo com un motivo", example = "Evidencias no válidas")
    private String motivoRechazo;

}
