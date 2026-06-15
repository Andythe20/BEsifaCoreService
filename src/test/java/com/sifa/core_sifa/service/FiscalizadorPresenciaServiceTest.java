package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.repository.IFiscalizadorPresenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FiscalizadorPresenciaServiceTest {

    @Mock
    private IFiscalizadorPresenciaRepository presenciaRepository;

    @InjectMocks
    private FiscalizadorPresenciaService presenciaService;

    @Captor
    private ArgumentCaptor<FiscalizadorPresencia> presenciaCaptor;

    @Test
    void registrarLatido_savesPresencia() {
        var request = new FiscalizadorHeartbeatRequest();
        request.setLatitud(-33.0f);
        request.setLongitud(-71.0f);
        request.setDeviceId("device-001");
        request.setMarca("Samsung");
        request.setModelo("Galaxy S24");

        presenciaService.registrarLatido("fiscalizador@test.cl", request);

        verify(presenciaRepository).save(presenciaCaptor.capture());
        var saved = presenciaCaptor.getValue();
        assertThat(saved.getEmailUsuario()).isEqualTo("fiscalizador@test.cl");
        assertThat(saved.getLatitud()).isEqualTo(-33.0f);
        assertThat(saved.getDeviceId()).isEqualTo("device-001");
    }

    @Test
    void obtenerFiscalizadoresActivos_returnsRecentlyActive() {
        var presencia = FiscalizadorPresencia.builder()
                .emailUsuario("fiscalizador@test.cl")
                .latitud(-33.0f)
                .longitud(-71.0f)
                .ultimaConexion(LocalDateTime.now())
                .build();
        var page = new PageImpl<>(List.of(presencia));

        given(presenciaRepository.findFiscalizadorActivos(any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(page);

        Page<FiscalizadorPresencia> result = presenciaService.obtenerFiscalizadoresActivos(PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getEmailUsuario()).isEqualTo("fiscalizador@test.cl");
    }
}
