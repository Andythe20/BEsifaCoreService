package com.sifa.core_sifa.dto.infraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO liviano dedicado exclusivamente al Dashboard.
 * Retorna el total de infracciones y la cantidad agrupada por estado (GROUP BY),
 * filtrable por rango de fechas. Por defecto, filtra por el día actual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resumen estadístico liviano para las tarjetas KPI del Dashboard")
public class DashboardEstadisticasDTO {

    @Schema(description = "Total de infracciones en el rango de fechas", example = "142")
    private Long totalInfracciones;

    @Schema(description = "Cantidad de infracciones agrupada por estado (pending, accepted, rejected, exported)",
            example = "{\"pending\": 50, \"accepted\": 60, \"rejected\": 12, \"exported\": 20}")
    private Map<String, Long> cantidadPorEstado;

    @Schema(description = "Fecha de inicio del filtro aplicado", example = "2026-05-31")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha de fin del filtro aplicado", example = "2026-05-31")
    private LocalDate fechaFin;
}
