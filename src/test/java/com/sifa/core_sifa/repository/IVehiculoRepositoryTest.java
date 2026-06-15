package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IVehiculoRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IVehiculoRepository vehiculoRepository;

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepository;

    @BeforeEach
    void setUp() {
        vehiculoRepository.deleteAll();
        propietarioRepository.deleteAll();

        var propietario = propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("12345678-9").nombres("JUAN").apellidos("PEREZ")
                .direccion("Calle 123").comuna("Santiago")
                .correo("juan@test.cl").telefono("+56900000000")
                .profesion("Ingeniero").estadoCivil("Soltero/a").edad(30)
                .build());

        vehiculoRepository.save(Vehiculo.builder()
                .patente("ABCD12").marca("TOYOTA").modelo("YARIS")
                .anioFabricacion(2020).color("BLANCO")
                .nroMotor("MOTOR123").nroSerie("SERIE456")
                .propietarioVehiculo(propietario)
                .build());
    }

    @Test
    void existsByNroMotor_cuandoExiste_retornaTrue() {
        assertThat(vehiculoRepository.existsByNroMotor("MOTOR123")).isTrue();
    }

    @Test
    void existsByNroMotor_cuandoNoExiste_retornaFalse() {
        assertThat(vehiculoRepository.existsByNroMotor("NO_EXISTE")).isFalse();
    }

    @Test
    void findByNroMotor_cuandoExiste_retornaVehiculo() {
        var result = vehiculoRepository.findByNroMotor("MOTOR123");

        assertThat(result).isPresent();
        assertThat(result.get().getPatente()).isEqualTo("ABCD12");
    }

    @Test
    void findByNroMotor_cuandoNoExiste_retornaEmpty() {
        var result = vehiculoRepository.findByNroMotor("NO_EXISTE");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByNroSerie_cuandoExiste_retornaTrue() {
        assertThat(vehiculoRepository.existsByNroSerie("SERIE456")).isTrue();
    }

    @Test
    void findByNroSerie_cuandoExiste_retornaVehiculo() {
        var result = vehiculoRepository.findByNroSerie("SERIE456");

        assertThat(result).isPresent();
        assertThat(result.get().getPatente()).isEqualTo("ABCD12");
    }
}
