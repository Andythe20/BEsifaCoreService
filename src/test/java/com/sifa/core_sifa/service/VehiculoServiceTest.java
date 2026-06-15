package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.VehiculoDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.Vehiculo;
import com.sifa.core_sifa.repository.IVehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock
    private IVehiculoRepository vehiculoRepository;

    @InjectMocks
    private VehiculoService vehiculoService;

    @Test
    void findAllVehiculos_returnsAll() {
        var vehiculo = createVehiculo();
        given(vehiculoRepository.findAll()).willReturn(List.of(vehiculo));

        var result = vehiculoService.findAllVehiculos();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatente()).isEqualTo("ABCD12");
    }

    @Test
    void findById_whenExists_returnsVehiculo() {
        var vehiculo = createVehiculo();
        given(vehiculoRepository.findById("ABCD12")).willReturn(Optional.of(vehiculo));

        var result = vehiculoService.findById("ABCD12");

        assertThat(result.getPatente()).isEqualTo("ABCD12");
        assertThat(result.getMarca()).isEqualTo("TOYOTA");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        given(vehiculoRepository.findById("INVALID")).willReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculoService.findById("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehículo no encontrado");
    }

    private Vehiculo createVehiculo() {
        var propietario = PropietarioVehiculo.builder()
                .rut("12345678-9")
                .nombres("JUAN")
                .apellidos("PEREZ")
                .build();
        return Vehiculo.builder()
                .patente("ABCD12")
                .marca("TOYOTA")
                .modelo("YARIS")
                .anioFabricacion(2020)
                .color("BLANCO")
                .propietarioVehiculo(propietario)
                .build();
    }
}
