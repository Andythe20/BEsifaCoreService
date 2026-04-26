package com.example.core_sifa.service;

import com.example.core_sifa.dto.citacion.CitacionCreateRequest;
import com.example.core_sifa.dto.citacion.CitacionResponse;
import com.example.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.example.core_sifa.model.Citacion;
import com.example.core_sifa.model.Infraccion;
import com.example.core_sifa.repository.ICitacionRepository;
import com.example.core_sifa.repository.IInfraccionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitacionService {

    private final ICitacionRepository citacionRepository;
    private final IInfraccionRepository infraccionRepository;

    @Transactional
    public CitacionResponse crearCitacion(CitacionCreateRequest request, String emailAdministrativoJpl) {
        log.info("Administrativo {} creando citación para la infracción ID: {}", emailAdministrativoJpl, request.getIdInfraccion());

        // Validar que la infracción exista
        Infraccion infraccion = infraccionRepository.findById(request.getIdInfraccion())
                .orElseThrow(() -> new IllegalArgumentException("Infracción no encontrada con ID: " + request.getIdInfraccion()));

        // Validar que la infraccion esté aprobada antes de generar una citación
        if (!"APROBADA".equalsIgnoreCase(infraccion.getEstado())) {
            throw new IllegalStateException("Solo se pueden generar citaciones para infracciones en estado APROBADA. Estado actual: " + infraccion.getEstado());
        }

        // Lógica de negocio (Relación 1:1): Validar que no exista ya una citación
        if (infraccion.getCitacion() != null) {
            throw new IllegalStateException("Esta infracción ya tiene una citación asignada con ID: " + infraccion.getCitacion().getIdCitacion());
        }

        // Construir la entidad Citacion
        Citacion nuevaCitacion = Citacion.builder()
                .fecha(request.getFecha()) // En este caso, el JPL sí define para cuándo es la cita
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
                .orElseThrow(() -> new IllegalArgumentException("Citación no encontrada con ID: " + idCitacion));

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
                .orElseThrow(() -> new IllegalArgumentException("Citación no encontrada con ID: " + idCitacion));

        return CitacionResponse.fromEntity(citacion);
    }
}