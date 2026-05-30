package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.repository.IFiscalizadorPresenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FiscalizadorPresenciaService {

    private final IFiscalizadorPresenciaRepository presenciaRepository;

    /**
     * Registra o actualiza el latido de un fiscalizador en terreno.
     */
    @Transactional
    public void registrarLatido(String email, FiscalizadorHeartbeatRequest request) {
        log.info("Recibiendo latido de presencia de: {}", email);

        FiscalizadorPresencia presencia = FiscalizadorPresencia.builder()
                .emailUsuario(email)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .deviceId(request.getDeviceId())
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .ultimaConexion(LocalDateTime.now())
                .build();

        presenciaRepository.save(presencia);
    }

    /**
     * Retorna la lista paginada de inspectores activos en los últimos 10 minutos.
     */
    @Transactional(readOnly = true)
    public Page<FiscalizadorPresencia> obtenerFiscalizadoresActivos(Pageable pageable) {
        // Tiempo de corte: Hace 10 minutos atrás
        LocalDateTime corte = LocalDateTime.now().minusMinutes(10);
        return presenciaRepository.findFiscalizadorActivos(corte, pageable);
    }
}
