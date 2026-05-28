package com.sifa.core_sifa.dto.infraccion;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Lugar de la infraccion", example = "Av. Maroto 410, Sector Comercial, Concón")
    @NotBlank(message = "El lugar de la infracción no puede estar vacío")
    private String lugar;

    @Schema(description = "Fecha exacta de la infracción", example = "2024-05-20T14:30:00")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @NotNull(message = "La fecha de la infracción no puede estar vacío y debe tener un formato válido")
    private LocalDateTime fecha;

    @Schema(description = "latitud de la ubicacion gps", example = "-32.9271")
    @NotNull(message = "La latitud es obligatoria")
    private Float latitud;

    @Schema(description = "longitud de la ubicacion gps", example = "-71.5212")
    @NotNull(message = "La longitud es obligatoria")
    private Float longitud;

    // Datos del Vehículo y Tipo (Referencias a otras entidades)
    @Schema(description = "Patente del vehiculo", example = "ABCD12")
    @NotBlank(message = "La patente del vehículo es obligatoria")
    @Size(min = 6, max = 6, message = "La patente debe tener un formato válido")
    private String patenteVehiculo;

    @Schema(description = "Tipo de infracción", example = "1")
    @NotNull(message = "Debe seleccionar un tipo de infracción")
    private Integer idTipoInfraccion;

    // Observaciones (Opcional)
    @Schema(description = "Campo que puede ser llenado cuando se esta registrando la infraccion", example = "El vehiculo estaba estacionado en medio de un paso cebra")
    private String observaciones;

    @Schema(description = "Fecha de citación al JPL", example = "2024-05-20T14:30:00")
    @NotNull(message = "La fecha de citación al JPL es obligatoria para el flujo empadronado")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime fechaCitacion;

}
