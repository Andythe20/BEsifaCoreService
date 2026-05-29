package com.sifa.core_sifa.dto.infraccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductividadFiscalizadorDTO {
    private String idFiscalizador; // El email del funcionario
    private Long cantidadInfracciones; // Cantidad cursada
}
