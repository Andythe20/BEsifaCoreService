package com.sifa.core_sifa.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sifa.core_sifa.model.TipoInfraccion;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoInfraccionDTO {

    private Integer id;
    private String nombre;
    private String disposicionInfringida;
    private Boolean habilitado;
    private LocalDate createdAt;
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
