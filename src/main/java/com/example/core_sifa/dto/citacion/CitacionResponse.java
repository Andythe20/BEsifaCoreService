package com.example.core_sifa.dto.citacion;

import com.example.core_sifa.model.Citacion;
import lombok.*;

import java.time.LocalDateTime;

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
