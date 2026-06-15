package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.dto.infraccion.CoordenadaDTO;
import com.sifa.core_sifa.dto.infraccion.ProductividadFiscalizadorDTO;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IInfraccionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IInfraccionRepository infraccionRepository;

    @Autowired
    private IVehiculoRepository vehiculoRepository;

    @Autowired
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepository;

    private Vehiculo vehiculo;
    private TipoInfraccion tipoInfraccion;

    @BeforeEach
    void setUp() {
        infraccionRepository.deleteAll();
        vehiculoRepository.deleteAll();
        tipoInfraccionRepository.deleteAll();
        propietarioRepository.deleteAll();

        var propietario = propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("11111111-1")
                .nombres("TEST")
                .apellidos("USER")
                .direccion("Dir 123")
                .comuna("Test")
                .correo("test@test.cl")
                .telefono("+56900000000")
                .profesion("Test")
                .estadoCivil("Soltero/a")
                .build());

        vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .patente("TEST11")
                .marca("TEST")
                .modelo("MODEL")
                .anioFabricacion(2020)
                .color("ROJO")
                .nroMotor("M123")
                .nroSerie("S456")
                .propietarioVehiculo(propietario)
                .build());

        tipoInfraccion = tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Test Infraccion")
                .disposicionInfringida("Art. Test")
                .habilitado(true)
                .build());
    }

    @Test
    void findByIdFiscalizadorOrderByFechaDesc_returnsFilteredResults() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user1@test.cl")
                .lugar("Lugar 1")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user2@test.cl")
                .lugar("Lugar 2")
                .latitud(-34.0f).longitud(-72.0f)
                .fecha(LocalDateTime.now())
                .estado("APROBADA")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        Page<Infraccion> result = infraccionRepository
                .findByIdFiscalizadorOrderByFechaDesc("user1@test.cl", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getIdFiscalizador()).isEqualTo("user1@test.cl");
    }

    @Test
    void findByVehiculoPatenteOrderByFechaDesc_returnsVehiculoInfracciones() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var result = infraccionRepository.findByVehiculoPatenteOrderByFechaDesc("TEST11");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVehiculo().getPatente()).isEqualTo("TEST11");
    }

    @Test
    void findByFechaBetweenOrderByFechaDesc_returnsInfraccionesInRange() {
        var now = LocalDateTime.now();
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(now)
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var start = now.minusHours(1);
        var end = now.plusHours(1);
        var result = infraccionRepository.findByFechaBetweenOrderByFechaDesc(start, end);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByFilters_withAllParams_returnsFilteredResults() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Av. Principal")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var start = LocalDateTime.now().minusDays(1);
        var end = LocalDateTime.now().plusDays(1);
        Page<Infraccion> result = infraccionRepository.findByFilters(
                start, end, "user@test.cl", "EN PROCESO", null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findByFilters_withSearchByPatente_returnsMatching() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        Page<Infraccion> result = infraccionRepository.findByFilters(
                null, null, null, null, "TEST", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findCoordenadasByFilters_returnsCoordenadas() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var result = infraccionRepository.findCoordenadasByFilters(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isInstanceOf(CoordenadaDTO.class);
    }

    @Test
    void findTopInfraccionesByFilters_returnsOrderedByCount() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var result = infraccionRepository.findTopInfraccionesByFilters(
                null, null, null, PageRequest.of(0, 3));

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().getNombre()).isEqualTo("Test Infraccion");
    }

    @Test
    void countEstadosByFilters_returnsGroupedCounts() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar 2")
                .latitud(-34.0f).longitud(-72.0f)
                .fecha(LocalDateTime.now())
                .estado("APROBADA")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var result = infraccionRepository.countEstadosByFilters(null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void countProductividadPorFiscalizador_returnsProductividad() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        var start = LocalDateTime.now().minusDays(1);
        var end = LocalDateTime.now().plusDays(1);
        var result = infraccionRepository.countProductividadPorFiscalizador(start, end);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isInstanceOf(ProductividadFiscalizadorDTO.class);
        assertThat(result.getFirst().getIdFiscalizador()).isEqualTo("user@test.cl");
    }

    @Test
    void findByFilters_withNullParams_returnsAll() {
        infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("user@test.cl")
                .lugar("Lugar")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipoInfraccion)
                .build());

        Page<Infraccion> result = infraccionRepository.findByFilters(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }
}
