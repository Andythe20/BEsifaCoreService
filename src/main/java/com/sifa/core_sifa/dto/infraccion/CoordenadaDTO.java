package com.sifa.core_sifa.dto.infraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordenadaDTO {
    @Schema(description = "Latitud de la infracción", example = "-32.9271")
    private Float latitud;
    @Schema(description = "Longitud de la infraccion", example = "-71.5212")
    private Float longitud;
}
