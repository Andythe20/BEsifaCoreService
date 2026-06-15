package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IPropietarioVehiculoRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepository;

    @BeforeEach
    void setUp() {
        propietarioRepository.deleteAll();
        propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("12345678-9").nombres("JUAN").apellidos("PEREZ")
                .direccion("Calle 123").comuna("Santiago")
                .correo("juan@test.cl").telefono("+56900000000")
                .profesion("Ingeniero").estadoCivil("Soltero/a").edad(30)
                .build());
    }

    @Test
    void findById_cuandoExiste_retornaPropietario() {
        var result = propietarioRepository.findById("12345678-9");

        assertThat(result).isPresent();
        assertThat(result.get().getNombres()).isEqualTo("JUAN");
    }

    @Test
    void findById_cuandoNoExiste_retornaEmpty() {
        var result = propietarioRepository.findById("99999999-9");

        assertThat(result).isEmpty();
    }

    @Test
    void save_y_findAll_ok() {
        var result = propietarioRepository.findAll();

        assertThat(result).hasSize(1);
    }
}
