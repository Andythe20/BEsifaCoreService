package com.example.core_sifa.dto.Citacion;

import com.example.core_sifa.model.Citacion;
import lombok.*;

import java.time.LocalDateTime;

/** Este DTO sólo será instanciado cuando llegue el json para crear la citacion.
 * No se requiere construirlo a partir de una entidad de la bbdd.*/
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionCreateRequest {
    private LocalDateTime fecha;
    private Integer id_infraccion;

}
