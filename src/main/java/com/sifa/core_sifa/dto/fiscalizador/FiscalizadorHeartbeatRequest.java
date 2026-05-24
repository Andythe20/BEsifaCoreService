package com.sifa.core_sifa.dto.fiscalizador;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class FiscalizadorHeartbeatRequest {
    @NotNull(message = "La latitud es obligatoria")
    private Float latitud;

    @NotNull(message = "La longitud es obligatoria")
    private Float longitud;
}