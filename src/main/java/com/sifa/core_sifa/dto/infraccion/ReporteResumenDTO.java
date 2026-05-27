package com.sifa.core_sifa.dto.infraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResumenDTO {
    @Schema(description = "Coordenadas de las infracciones")
    private List<CoordenadaDTO> coordenadas;
    @Schema(description = "Top 5 de infracciones más comunes")
    private List<TopInfraccionDTO> topInfracciones;
    @Schema(description = "Cantidad de estados de infracción")
    private Map<String, Long> estados;
    @Schema(description = "Total de infracciones")
    private Long totalCount;
}
