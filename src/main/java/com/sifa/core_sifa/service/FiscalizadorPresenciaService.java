package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.repository.IFiscalizadorPresenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
                .ultimaConexion(LocalDateTime.now())
                .build();

        presenciaRepository.save(presencia);
    }

    /**
     * Retorna la lista de inspectores activos en los últimos 10 minutos.
     */
    @Transactional(readOnly = true)
    public List<FiscalizadorPresencia> obtenerFiscalizadoresActivos() {
        // Tiempo de corte: Hace 10 minutos atrás
        LocalDateTime corte = LocalDateTime.now().minusMinutes(10);
        return presenciaRepository.findFiscalizadorActivos(corte);
    }
}
