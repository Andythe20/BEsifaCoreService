package com.sifa.core_sifa.dto.infraccion;

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
    private List<CoordenadaDTO> coordenadas;
    private List<TopInfraccionDTO> topInfracciones;
    private Map<String, Long> estados;
    private Long totalCount;
}
