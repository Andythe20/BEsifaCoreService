package com.example.core_sifa.dto.Citacion;

import com.example.core_sifa.model.Citacion;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionResponse {

    private Integer id_citacion;
    private LocalDateTime fecha;
    private Integer id_infraccion;

    public static CitacionResponse fromEntity(Citacion citacion) {
        return CitacionResponse.builder()
                .id_citacion(citacion.getId_citacion())
                .fecha(citacion.getFecha())
                .id_infraccion(citacion.getInfraccion().getId_infraccion())
                .build();
    }

}
