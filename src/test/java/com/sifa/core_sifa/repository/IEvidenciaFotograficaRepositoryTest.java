package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.EvidenciaFotografica;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.PropietarioVehiculo;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IEvidenciaFotograficaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IEvidenciaFotograficaRepository evidenciaRepository;

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
        evidenciaRepository.deleteAll();
        infraccionRepository.deleteAll();
        vehiculoRepository.deleteAll();
        tipoInfraccionRepository.deleteAll();
        propietarioRepository.deleteAll();

        var propietario = propietarioRepository.save(PropietarioVehiculo.builder()
                .rut("11111111-1").nombres("TEST").apellidos("USER")
                .direccion("Dir 123").comuna("Test")
                .correo("test@test.cl").telefono("+56900000000")
                .profesion("Test").estadoCivil("Soltero/a")
                .build());

        var vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .patente("TEST11").marca("TEST").modelo("MODEL")
                .anioFabricacion(2020).color("ROJO")
                .nroMotor("MOTOR123").nroSerie("SERIE456")
                .propietarioVehiculo(propietario).build());

        var tipo = tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Test").disposicionInfringida("Art. 1").habilitado(true).build());

        infraccion = infraccionRepository.save(Infraccion.builder()
                .idFiscalizador("fiscalizador@test.cl")
                .lugar("Test").latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now()).estado("EN PROCESO")
                .vehiculo(vehiculo).tipoInfraccion(tipo).build());

        evidenciaRepository.save(EvidenciaFotografica.builder()
                .url("https://s3.test.cl/foto1.jpg").infraccion(infraccion).build());
        evidenciaRepository.save(EvidenciaFotografica.builder()
                .url("https://s3.test.cl/foto2.jpg").infraccion(infraccion).build());
    }

    @Test
    void existsByUrl_cuandoExiste_retornaTrue() {
        assertThat(evidenciaRepository.existsByUrl("https://s3.test.cl/foto1.jpg")).isTrue();
    }

    @Test
    void existsByUrl_cuandoNoExiste_retornaFalse() {
        assertThat(evidenciaRepository.existsByUrl("https://s3.test.cl/no-existe.jpg")).isFalse();
    }

    @Test
    void findByUrl_cuandoExiste_retornaEvidencia() {
        var result = evidenciaRepository.findByUrl("https://s3.test.cl/foto1.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().getInfraccion().getIdInfraccion()).isEqualTo(infraccion.getIdInfraccion());
    }

    @Test
    void findByUrl_cuandoNoExiste_retornaEmpty() {
        var result = evidenciaRepository.findByUrl("https://s3.test.cl/no-existe.jpg");

        assertThat(result).isEmpty();
    }
}
