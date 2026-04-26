package com.example.core_sifa.dto;

import com.example.core_sifa.model.Vehiculo;
import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoDTO {
    private String patente;
    private String marca;
    private String modelo;
    private Integer anio_fabricacion;
    private String color;
    private String nro_motor;
    private String nro_serie;
    private String propietario;
    private String rut;

    public static VehiculoDTO fromEntity(Vehiculo vehiculo) {
        return VehiculoDTO.builder()
                .patente(vehiculo.getPatente())
                .marca(vehiculo.getMarca())
                .modelo(vehiculo.getModelo())
                .anio_fabricacion(vehiculo.getAnio_fabricacion())
                .color(vehiculo.getColor())
                .nro_motor(vehiculo.getNro_motor())
                .nro_serie(vehiculo.getNro_serie())
                .propietario(vehiculo.getPropietario_vehiculo().getNombres() + " " + vehiculo.getPropietario_vehiculo().getApellidos())
                .rut(vehiculo.getPropietario_vehiculo().getRut())
                .build();
    }
}
