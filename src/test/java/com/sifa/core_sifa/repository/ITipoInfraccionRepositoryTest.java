package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.TipoInfraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ITipoInfraccionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @BeforeEach
    void setUp() {
        tipoInfraccionRepository.deleteAll();
        tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Mal Estacionado").disposicionInfringida("Art. 154").habilitado(true).build());
        tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Exceso Velocidad").disposicionInfringida("Art. 145").habilitado(true).build());
        tipoInfraccionRepository.save(TipoInfraccion.builder()
                .nombre("Obsoleto").disposicionInfringida("Art. X").habilitado(false).build());
    }

    @Test
    void findByHabilitadoTrue_retornaSoloHabilitados() {
        var result = tipoInfraccionRepository.findByHabilitadoTrue(PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result.getContent())
                .extracting(TipoInfraccion::getNombre)
                .containsExactlyInAnyOrder("Mal Estacionado", "Exceso Velocidad");
    }

    @Test
    void findById_retornaTipo() {
        var all = tipoInfraccionRepository.findAll();
        var id = all.getFirst().getIdTipoInfraccion();

        var result = tipoInfraccionRepository.findById(id);

        assertThat(result).isPresent();
    }
}
