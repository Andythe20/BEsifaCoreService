package com.sifa.core_sifa.dto.citacion;

import lombok.*;

import java.time.LocalDateTime;

import com.sifa.core_sifa.model.Citacion;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionResponse {

    private Integer idCitacion;
    private LocalDateTime fecha;
    private Integer idInfraccion;

    public static CitacionResponse fromEntity(Citacion citacion) {
        return CitacionResponse.builder()
                .idCitacion(citacion.getIdCitacion())
                .fecha(citacion.getFecha())
                .idInfraccion(citacion.getInfraccion().getIdInfraccion())
                .build();
    }

}
