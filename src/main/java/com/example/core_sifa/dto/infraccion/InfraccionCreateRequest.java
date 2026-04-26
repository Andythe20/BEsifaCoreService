package com.example.core_sifa.dto.infraccion;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraccionCreateRequest {

    // Datos de Ubicación (Obligatorios desde el GPS y formulario del móvil)
    @NotBlank(message = "El lugar de la infracción no puede estar vacío")
    private String lugar;

    @NotNull(message = "La fecha de la infracción no puede estar vacío y debe tener un formato válido")
    private LocalDateTime fecha;

    @NotNull(message = "La latitud es obligatoria")
    private Float latitud;

    @NotNull(message = "La longitud es obligatoria")
    private Float longitud;

    // Datos del Vehículo y Tipo (Referencias a otras entidades)
    @NotBlank(message = "La patente del vehículo es obligatoria")
    @Size(min = 6, max = 6, message = "La patente debe tener un formato válido")
    private String patenteVehiculo;

    @NotNull(message = "Debe seleccionar un tipo de infracción")
    private Integer idTipoInfraccion;

    // Observaciones (Opcional)
    private String observaciones;

    // Evidencias (Depende de cómo manejes las imágenes)
    @NotNull(message = "Debe incluir al menos una evidencia fotográfica")
    @Size(min = 1, message = "Debe subir al menos una foto")
    private List<String> urlsEvidencias;
}
