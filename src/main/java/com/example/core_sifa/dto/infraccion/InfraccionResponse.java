package com.example.core_sifa.dto.infraccion;

import com.example.core_sifa.model.EvidenciaFotografica;
import com.example.core_sifa.model.Infraccion;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraccionResponse {

    private String patente;
    private String lugar;
    private String observaciones;
    private LocalDateTime fecha;
    private Float longitud;
    private Float latitud;
    private List<String> evidenciaFotograficas;

    public static InfraccionResponse fromEntity(Infraccion infraccion){
        return InfraccionResponse.builder()
                .patente(infraccion.getVehiculo().getPatente())
                .lugar(infraccion.getLugar())
                .observaciones(infraccion.getObservaciones())
                .fecha(infraccion.getFecha())
                .longitud(infraccion.getLongitud())
                .latitud(infraccion.getLatitud())
                .evidenciaFotograficas(
                        infraccion.getEvidenciasFotograficas() != null
                                ? infraccion.getEvidenciasFotograficas().stream()
                                  .map(EvidenciaFotografica::getUrl) // Extraemos solo el campo 'url'
                                  .collect(Collectors.toList()) // Lo empaquetamos en una nueva List<String>
                                : Collections.emptyList() // Si es null, devolvemos una lista vacía en lugar de null
                )
                .build();
    }
}
