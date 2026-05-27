package com.sifa.core_sifa.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sifa.core_sifa.model.TipoInfraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoInfraccionDTO {

    @Schema(description = "Identificador del tipo de infraccion", example = "1")
    private Integer id;
    @Schema(description = "Nombre del tipo de infraccion", example = "Exceso de velocidad")
    private String nombre;
    @Schema(description = "Artículo de la ley correspondiente", example = "Art. 154, Ley de Tránsito 18.290")
    private String disposicionInfringida;
    @Schema(description = "Campo para saber si esta habilitado", example = "true")
    private Boolean habilitado;
    @Schema(description = "Hora a la que se crea", example = "2026-05-26 15:19:36.828831")
    private LocalDate createdAt;
    @Schema(description = "Hora a la que se actualiza", example = "2026-05-26 15:19:36.828831")
    private LocalDateTime updatedAt;

    public static TipoInfraccionDTO fromEntity(TipoInfraccion tipoInfraccion) {
        return TipoInfraccionDTO.builder()
                .id(tipoInfraccion.getIdTipoInfraccion())
                .nombre(tipoInfraccion.getNombre())
                .disposicionInfringida(tipoInfraccion.getDisposicionInfringida())
                .habilitado(tipoInfraccion.getHabilitado())
                .createdAt(tipoInfraccion.getCreatedAt())
                .updatedAt(tipoInfraccion.getUpdatedAt())
                .build();
    }

    public TipoInfraccion toEntity() {
        return TipoInfraccion.builder()
                .idTipoInfraccion(this.id)
                .nombre(this.nombre)
                .disposicionInfringida(this.disposicionInfringida)
                .habilitado(this.habilitado != null ? this.habilitado : true)
                .build();
    }
}
