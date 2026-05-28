package com.sifa.core_sifa.service;

import org.springframework.transaction.annotation.Transactional;

import com.sifa.core_sifa.dto.citacion.CitacionResponse;
import com.sifa.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.Citacion;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.repository.ICitacionRepository;
import com.sifa.core_sifa.repository.IInfraccionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitacionService {

    private final ICitacionRepository citacionRepository;
    private final IInfraccionRepository infraccionRepository;

    @Transactional
    public CitacionResponse crearCitacion(Integer idInfraccion, LocalDateTime fechaCitacion) {
        log.info("Creando citación para la infracción ID: {}", idInfraccion);

        // Validar que la infracción exista
        Infraccion infraccion = infraccionRepository.findById(idInfraccion)
                .orElseThrow(() -> new ResourceNotFoundException("Infracción no encontrada con ID: " + idInfraccion));

        // Lógica de negocio (Relación 1:1): Validar que no exista ya una citación
        if (infraccion.getCitacion() != null) {
            throw new IllegalStateException("Esta infracción ya tiene una citación asignada con ID: " + infraccion.getCitacion().getIdCitacion());
        }

        // Construir la entidad Citacion
        Citacion nuevaCitacion = Citacion.builder()
                .fecha(fechaCitacion)
                .infraccion(infraccion)
                .build();

        // Guardar en base de datos
        Citacion citacionGuardada = citacionRepository.save(nuevaCitacion);
        log.info("Citación ID {} creada exitosamente", citacionGuardada.getIdCitacion());

        // Retornar el DTO
        return CitacionResponse.fromEntity(citacionGuardada);
    }

    @Transactional
    public CitacionResponse actualizarCitacion(Integer idCitacion, CitacionUpdateRequest request, String emailAdministrativoJpl) {
        log.info("Administrativo {} actualizando citación {}.", emailAdministrativoJpl, idCitacion);

        // Buscar la citacion
        Citacion citacion = citacionRepository.findById(idCitacion)
                .orElseThrow(() -> new ResourceNotFoundException("Citación no encontrada con ID: " + idCitacion));

        // No se puede cambiar la fecha si la cita original ya ocurrió en el pasado
        if (citacion.getFecha().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("No se puede reprogramar una citación cuya fecha ya ha pasado.");
        }

        // No agendar hacia el pasado
        if (request.getFecha().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha de citación no puede estar en el pasado.");
        }

        // actualizar campos
        citacion.setFecha(request.getFecha());

        // guardar cambios
        Citacion citacionActualizada = citacionRepository.save(citacion);

        log.info("Citacion actualizada exitosamente con ID {}", idCitacion);
        return CitacionResponse.fromEntity(citacionActualizada);
    }


    @Transactional(readOnly = true)
    public CitacionResponse findById(Integer idCitacion) {
        log.info("Buscando citación con ID: {}", idCitacion);
        Citacion citacion = citacionRepository.findById(idCitacion)
                .orElseThrow(() -> new ResourceNotFoundException("Citación no encontrada con ID: " + idCitacion));

        return CitacionResponse.fromEntity(citacion);
    }
}