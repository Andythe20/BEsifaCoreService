package com.sifa.core_sifa.integration;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import com.sifa.core_sifa.repository.IInfraccionRepository;
import com.sifa.core_sifa.repository.IPropietarioVehiculoRepository;
import com.sifa.core_sifa.repository.ITipoInfraccionRepository;
import com.sifa.core_sifa.repository.IVehiculoRepository;
import com.sifa.core_sifa.service.CitacionService;
import com.sifa.core_sifa.service.IStorageService;
import com.sifa.core_sifa.service.infraccion.InfraccionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InfraccionFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InfraccionServiceImpl infraccionService;

    @Autowired
    private CitacionService citacionService;

    @Autowired
    private IInfraccionRepository infraccionRepository;

    @Autowired
    private IVehiculoRepository vehiculoRepository;

    @Autowired
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepository;

    @Autowired
    private IStorageService storageService;

    @BeforeEach
    void setUp() {
        infraccionRepository.deleteAll();
        vehiculoRepository.deleteAll();
        tipoInfraccionRepository.deleteAll();
        propietarioRepository.deleteAll();

        var propietario = propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("33333333-3")
                .nombres("INTEGRATION")
                .apellidos("TEST")
                .direccion("Dir Integration")
                .comuna("Test")
                .correo("integration@test.cl")
                .telefono("+56922222222")
                .profesion("Test")
                .estadoCivil("Soltero/a")
                .edad(30)
                .build());

        vehiculoRepository.save(Vehiculo.builder()
                .patente("INTG11")
                .marca("TEST")
                .modelo("INTEGRATION")
                .anioFabricacion(2023)
                .color("NEGRO")
                .tipo("Sedán")
                .nroMotor("MOTOR-INT")
                .nroSerie("SERIE-INT")
                .propietarioVehiculo(propietario)
                .build());

        tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Exceso de Velocidad")
                .disposicionInfringida("Art. 150")
                .habilitado(true)
                .build());
    }

    @Test
    void crearYBuscarInfraccion_flowCompleto() {
        var request = com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest.builder()
                .patenteVehiculo("INTG11")
                .idTipoInfraccion(tipoInfraccionRepository.findAll().getFirst().getIdTipoInfraccion())
                .lugar("Av. Integration 789")
                .latitud(-33.5f)
                .longitud(-71.5f)
                .fecha(LocalDateTime.now())
                .fechaCitacion(LocalDateTime.now().plusDays(10))
                .observaciones("Test de integración")
                .build();

        List<MultipartFile> fotos = List.of(
                new MockMultipartFile("fotos", "foto1.jpg", "image/jpeg", "img1".getBytes()),
                new MockMultipartFile("fotos", "foto2.jpg", "image/jpeg", "img2".getBytes())
        );

        InfraccionResponse creada = infraccionService.crearInfraccion(request, fotos, "fiscalizador@test.cl");

        assertThat(creada).isNotNull();
        assertThat(creada.getId()).isNotNull();
        assertThat(creada.getStatus()).isEqualTo("pending");
        assertThat(creada.getEvidenceUrls()).hasSize(2);

        InfraccionResponse buscada = infraccionService.findById(Integer.parseInt(creada.getId()));
        assertThat(buscada.getId()).isEqualTo(creada.getId());
        assertThat(buscada.getIdFiscalizador()).isEqualTo("fiscalizador@test.cl");

        var procesada = infraccionService.procesarInfraccionPorJpl(
                Integer.parseInt(creada.getId()),
                com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest.builder()
                        .estado("APROBADA")
                        .build(),
                "jpl@test.cl");

        assertThat(procesada.getStatus()).isEqualTo("accepted");

        infraccionRepository.deleteAll();
    }
}
