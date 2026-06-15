package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IFiscalizadorPresenciaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IFiscalizadorPresenciaRepository presenciaRepository;

    @BeforeEach
    void setUp() {
        presenciaRepository.deleteAll();

        presenciaRepository.save(FiscalizadorPresencia.builder()
                .emailUsuario("fiscalizador1@test.cl")
                .ultimaConexion(LocalDateTime.now().minusMinutes(1))
                .latitud(-33.0f).longitud(-71.0f)
                .deviceId("device-1").marca("Google").modelo("Pixel 7")
                .build());

        presenciaRepository.save(FiscalizadorPresencia.builder()
                .emailUsuario("fiscalizador1@test.cl")
                .ultimaConexion(LocalDateTime.now().minusMinutes(2))
                .latitud(-33.01f).longitud(-71.01f)
                .deviceId("device-1").marca("Google").modelo("Pixel 7")
                .build());

        presenciaRepository.save(FiscalizadorPresencia.builder()
                .emailUsuario("fiscalizador2@test.cl")
                .ultimaConexion(LocalDateTime.now().minusMinutes(15))
                .latitud(-33.02f).longitud(-71.02f)
                .deviceId("device-2").marca("Samsung").modelo("Galaxy S24")
                .build());
    }

    @Test
    void findFiscalizadorActivos_retornaSoloActivosRecientes() {
        var corte = LocalDateTime.now().minusMinutes(10);

        var result = presenciaRepository.findFiscalizadorActivos(corte, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getEmailUsuario()).isEqualTo("fiscalizador1@test.cl");
    }

    @Test
    void findFiscalizadorActivos_retornaUnRegistroPorUsuario() {
        var corte = LocalDateTime.now().minusMinutes(10);

        var result = presenciaRepository.findFiscalizadorActivos(corte, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(FiscalizadorPresencia::getEmailUsuario)
                .containsOnly("fiscalizador1@test.cl");
    }

    @Test
    void findAll_retornaTodosLosRegistros() {
        var result = presenciaRepository.findAll();

        assertThat(result).hasSize(3);
    }
}
