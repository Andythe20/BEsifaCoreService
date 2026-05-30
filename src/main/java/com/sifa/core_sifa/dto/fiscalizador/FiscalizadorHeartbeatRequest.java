package com.sifa.core_sifa.dto.fiscalizador;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class FiscalizadorHeartbeatRequest {
    @Schema(description = "latitud de la ubicacion gps")
    @NotNull(message = "La latitud es obligatoria")
    private Float latitud;

    @Schema(description = "longitud de la ubicacion gps")
    @NotNull(message = "La longitud es obligatoria")
    private Float longitud;

    @Schema(description = "Id del dispositivo móvil")
    @NotNull(message = "El id del dispositivo es obligatorio")
    private String deviceId;

    @Schema(description = "Marca del dispositivo móvil")
    @NotNull(message = "La marca del dispositivo es obligatoria")
    private String marca;

    @Schema(description = "Modelo del dispositivo móvil")
    @NotNull(message = "El modelo del dispositivo es obligatorio")
    private String modelo;
}