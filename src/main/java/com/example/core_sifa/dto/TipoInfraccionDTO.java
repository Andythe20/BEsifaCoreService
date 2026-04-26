package com.example.core_sifa.dto;

import com.example.core_sifa.model.TipoInfraccion;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoInfraccionDTO {

    private Integer id;
    private String nombre;

    public static TipoInfraccionDTO fromEntity(TipoInfraccion tipoInfraccion) {
        return TipoInfraccionDTO.builder()
                .id(tipoInfraccion.getIdTipoInfraccion())
                .nombre(tipoInfraccion.getNombre())
                .build();
    }
}
