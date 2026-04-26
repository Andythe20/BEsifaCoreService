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
                .anio_fabricacion(vehiculo.getAnioFabricacion())
                .color(vehiculo.getColor())
                .nro_motor(vehiculo.getNroMotor())
                .nro_serie(vehiculo.getNroSerie())
                .propietario(vehiculo.getPropietarioVehiculo().getNombres() + " " + vehiculo.getPropietarioVehiculo().getApellidos())
                .rut(vehiculo.getPropietarioVehiculo().getRut())
                .build();
    }
}
