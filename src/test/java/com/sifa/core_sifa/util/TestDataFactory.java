package com.sifa.core_sifa.util;

import com.sifa.core_sifa.model.Citacion;
import com.sifa.core_sifa.model.EvidenciaFotografica;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;

import java.time.LocalDateTime;
import java.util.List;

public class TestDataFactory {

    public static PropietarioVehiculo createPropietario() {
        return PropietarioVehiculo.builder()
                .rut("12345678-9")
                .nombres("JUAN")
                .apellidos("PEREZ GONZALEZ")
                .direccion("Calle Falsa 123")
                .comuna("Viña del Mar")
                .correo("juan.perez@email.com")
                .telefono("+56912345678")
                .profesion("Ingeniero")
                .estadoCivil("Casado/a")
                .edad(45)
                .build();
    }

    public static Vehiculo createVehiculo(PropietarioVehiculo propietario) {
        return Vehiculo.builder()
                .patente("ABCD12")
                .marca("TOYOTA")
                .modelo("YARIS")
                .anioFabricacion(2020)
                .color("BLANCO")
                .tipo("Sedán")
                .nroMotor("MOTOR123")
                .nroSerie("SERIE456")
                .propietarioVehiculo(propietario)
                .build();
    }

    public static TipoInfraccion createTipoInfraccion() {
        return TipoInfraccion.builder()
                .nombre("Mal Estacionado")
                .disposicionInfringida("Art. 154, Ley de Tránsito 18.290")
                .habilitado(true)
                .build();
    }

    public static Infraccion createInfraccion(Vehiculo vehiculo, TipoInfraccion tipoInfraccion) {
        return Infraccion.builder()
                .idFiscalizador("fiscalizador@test.cl")
                .idUsuarioJpl(null)
                .lugar("Av. Principal 123")
                .latitud(-33.0f)
                .longitud(-71.0f)
                .observaciones("Test observacion")
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo)
                .tipoInfraccion(tipoInfraccion)
                .build();
    }

    public static Infraccion createInfraccionConEstado(Vehiculo vehiculo, TipoInfraccion tipoInfraccion, String estado) {
        Infraccion infraccion = createInfraccion(vehiculo, tipoInfraccion);
        infraccion.setEstado(estado);
        return infraccion;
    }

    public static EvidenciaFotografica createEvidencia(String url, Infraccion infraccion) {
        return EvidenciaFotografica.builder()
                .url(url)
                .infraccion(infraccion)
                .build();
    }

    public static Citacion createCitacion(Infraccion infraccion, LocalDateTime fecha) {
        return Citacion.builder()
                .fecha(fecha)
                .infraccion(infraccion)
                .build();
    }

    public static List<Infraccion> createInfraccionList(Vehiculo vehiculo, TipoInfraccion tipoInfraccion, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    var estados = List.of("EN PROCESO", "APROBADA", "RECHAZADA", "EXPORTADA");
                    return Infraccion.builder()
                            .idFiscalizador("fiscalizador" + i + "@test.cl")
                            .lugar("Lugar " + i)
                            .latitud(-33.0f + i)
                            .longitud(-71.0f + i)
                            .fecha(LocalDateTime.now().minusDays(i))
                            .estado(estados.get(i % estados.size()))
                            .vehiculo(vehiculo)
                            .tipoInfraccion(tipoInfraccion)
                            .build();
                })
                .toList();
    }
}
