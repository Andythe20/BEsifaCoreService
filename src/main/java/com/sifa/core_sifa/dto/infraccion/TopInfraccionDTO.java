package com.sifa.core_sifa.dto.infraccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopInfraccionDTO {
    private String nombre;
    private Long cantidad;
}
