package com.sifa.core_sifa.dto.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequestDTO {
    @NotNull(message = "El email del usuario es obligatorio")
    private String emailUsuario;

    @NotBlank(message = "La accion es obligatoria")
    private String accion;

    private String tablaAfectada;

    private String idRegistroAfectado;

    @NotNull(message = "Los detalles son obligatorios")
    private Map<String, Object> detalles;
}
