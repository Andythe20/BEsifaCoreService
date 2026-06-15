package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.Citacion;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ICitacionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ICitacionRepository citacionRepository;

    @Autowired
    private IInfraccionRepository infraccionRepository;

    @Autowired
    private IVehiculoRepository vehiculoRepository;

    @Autowired
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepository;

    private Infraccion infraccion;

    @BeforeEach
    void setUp() {
        citacionRepository.deleteAll();
        infraccionRepository.deleteAll();
        vehiculoRepository.deleteAll();
        tipoInfraccionRepository.deleteAll();
        propietarioRepository.deleteAll();

        var propietario = propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("22222222-2")
                .nombres("CITACION")
                .apellidos("TEST")
                .direccion("Dir 456")
                .comuna("Test")
                .correo("citacion@test.cl")
                .telefono("+56911111111")
                .profesion("Test")
                .estadoCivil("Soltero/a")
                .build());

        var vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .patente("CITA11")
                .marca("TEST")
                .modelo("MODEL")
                .anioFabricacion(2021)
                .color("AZUL")
                .nroMotor("M789")
                .nroSerie("S012")
                .propietarioVehiculo(propietario)
                .build());

        var tipoInfraccion = tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Citacion Test")
                .disposicionInfringida("Art. Test")
                .habilitado(true)
                .build());

        infraccion = infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("fiscalizador@test.cl")
                .lugar("Av. Test 456")
                .latitud(-33.5f).longitud(-71.5f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(vehiculo)
                .tipoInfraccion(tipoInfraccion)
                .build());
    }

    @Test
    void findByFilters_withNoFilters_returnsAll() {
        citacionRepository.save(Citacion.builder()
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build());

        Page<Citacion> result = citacionRepository.findByFilters(
                null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }

    @Test
    void findByFilters_withSearchByPatente_returnsMatching() {
        citacionRepository.save(Citacion.builder()
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build());

        Page<Citacion> result = citacionRepository.findByFilters(
                null, null, "CITA", PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }

    @Test
    void findByFilters_withDateRange_returnsFiltered() {
        var futureDate = LocalDateTime.now().plusDays(7);
        citacionRepository.save(Citacion.builder()
                .fecha(futureDate)
                .infraccion(infraccion)
                .build());

        var start = LocalDateTime.now().plusDays(6);
        var end = LocalDateTime.now().plusDays(8);
        Page<Citacion> result = citacionRepository.findByFilters(
                start, end, null, PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }

    @Test
    void saveAndFindById_worksCorrectly() {
        var citacion = citacionRepository.save(Citacion.builder()
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build());

        var found = citacionRepository.findById(citacion.getIdCitacion());

        assertThat(found).isPresent();
        assertThat(found.get().getInfraccion().getIdInfraccion()).isEqualTo(infraccion.getIdInfraccion());
    }
}
