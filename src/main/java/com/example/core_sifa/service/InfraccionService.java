package com.example.core_sifa.service;

import com.example.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.example.core_sifa.dto.infraccion.InfraccionResponse;
import com.example.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.example.core_sifa.exception.ResourceNotFoundException;
import com.example.core_sifa.model.EvidenciaFotografica;
import com.example.core_sifa.model.Infraccion;
import com.example.core_sifa.repository.IInfraccionRepository;
import com.example.core_sifa.repository.ITipoInfraccionRepository;
import com.example.core_sifa.repository.IVehiculoRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfraccionService {

        private final IInfraccionRepository infraccionRepository;
        private final IVehiculoRepository vehiculoRepository;
        private final ITipoInfraccionRepository tipoInfraccionRepository;
        private final IStorageService storageService;

        @Transactional(readOnly = true)
        public List<InfraccionResponse> findAllInfracciones() {
                log.info("Listando todas las infracciones");

                return infraccionRepository.findAll()
                                .stream()
                                .map(InfraccionResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public InfraccionResponse findById(Integer idInfraccion) {
                log.info("Buscando infraccion con id: {}", idInfraccion);
                Infraccion infraccion = infraccionRepository.findById(idInfraccion)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infraccion no encontrada o inexistente"));

                return InfraccionResponse.fromEntity(infraccion);
        }

        @Transactional(readOnly = true)
        public List<InfraccionResponse> findByIdFiscalizador(String idFiscalizador) {
                log.info("Buscando infracciones por id Fiscalizador: {}", idFiscalizador);

                List<Infraccion> listaInfracciones = infraccionRepository.findByIdFiscalizador(idFiscalizador);

                return listaInfracciones.stream()
                                .map(InfraccionResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<InfraccionResponse> findByVehiculoPatente(String vehiculoPatente) {
                log.info("Buscando infracciones por patente: {}", vehiculoPatente);

                List<Infraccion> listaInfracciones = infraccionRepository.findByVehiculoPatente(vehiculoPatente);

                return listaInfracciones.stream()
                                .map(InfraccionResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public InfraccionResponse crearInfraccion(InfraccionCreateRequest request, List<MultipartFile> fotos,
                        String idFiscalizador) {
                log.info("Iniciando creación de infracción para patente: {}", request.getPatenteVehiculo());

                // Validar que el vehículo exista
                var vehiculo = vehiculoRepository.findById(request.getPatenteVehiculo())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vehículo no encontrado con patente: " + request.getPatenteVehiculo()));

                // Validar que el tipo de infracción sea correcto
                var tipoInfraccion = tipoInfraccionRepository.findById(request.getIdTipoInfraccion())
                                .orElseThrow(() -> new IllegalArgumentException("Tipo de infracción no válido"));

                // Construir la entidad base
                Infraccion nuevaInfraccion = Infraccion.builder()
                                .idFiscalizador(idFiscalizador) // Viene del API Gateway (Extraído en el Controller)
                                .idUsuarioJpl(null) // Aún no asignado
                                .lugar(request.getLugar())
                                .latitud(request.getLatitud())
                                .longitud(request.getLongitud())
                                .observaciones(request.getObservaciones())
                                .fecha(request.getFecha())
                                .estado("EN PROCESO")
                                .vehiculo(vehiculo)
                                .tipoInfraccion(tipoInfraccion)
                                // citacion queda en null inicialmente
                                .build();

                // Procesar los archivos recibidos
                if (fotos == null || fotos.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Es obligatorio adjuntar al menos una fotografía de evidencia.");
                }

                List<String> uploadedUrls = new java.util.ArrayList<>();

                try {

                        // Subir archivos usando el storage activo
                        // dev -> mock
                        // prod -> S3 real
                        List<String> urls = storageService.uploadFiles(
                                        fotos,
                                        request.getPatenteVehiculo());

                        uploadedUrls.addAll(urls);

                        List<EvidenciaFotografica> evidencias = urls.stream()
                                        .map(url -> EvidenciaFotografica.builder()
                                                        .url(url)
                                                        .infraccion(nuevaInfraccion)
                                                        .build())
                                        .collect(Collectors.toList());

                        nuevaInfraccion.setEvidenciasFotograficas(evidencias);

                        // Guardar en base de datos
                        Infraccion infraccionGuardada = infraccionRepository.save(nuevaInfraccion);

                        log.info("Infracción creada exitosamente con ID: {}",
                                        infraccionGuardada.getIdInfraccion());

                        return InfraccionResponse.fromEntity(infraccionGuardada);

                } catch (Exception e) {

                        log.error("Error creando infracción. Rollback archivos storage", e);

                        // Si falla la BD eliminamos archivos subidos
                        uploadedUrls.forEach(url -> {
                                try {
                                        storageService.deleteFile(url);
                                } catch (Exception ex) {
                                        log.error("Error eliminando archivo: {}", url, ex);
                                }
                        });

                        throw e;
                }
        }

        @Transactional
        public InfraccionResponse procesarInfraccionPorJpl(Integer idInfraccion, InfraccionUpdateRequest request,
                        String idAdministrativoJpl) {
                log.info("Administrativo JPL {} procesando infracción ID: {}", idAdministrativoJpl, idInfraccion);

                // Buscar la infracción existente
                Infraccion infraccion = infraccionRepository.findById(idInfraccion)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infracción no encontrada con ID: " + idInfraccion));

                // Validar que no se intente reprocesar una infracción ya cerrada
                if ("APROBADA".equalsIgnoreCase(infraccion.getEstado())
                                || "RECHAZADA".equalsIgnoreCase(infraccion.getEstado())) {
                        throw new IllegalStateException(
                                        "La infracción ya fue procesada anteriormente y no puede ser modificada.");
                }

                // Si es rechazada, debe tener motivo
                if ("RECHAZADA".equalsIgnoreCase(request.getEstado()) &&
                                (request.getMotivoRechazo() == null || request.getMotivoRechazo().trim().isEmpty())) {
                        throw new IllegalArgumentException("Debe ingresar un motivo de rechazo válido.");
                }

                // Actualizar los campos enviados por el frontend
                infraccion.setEstado(request.getEstado().toUpperCase());
                infraccion.setMotivoRechazo(request.getMotivoRechazo());

                // Datos generados y controlados por el Backend
                infraccion.setFechaResolucion(java.time.LocalDateTime.now());

                // Asignamos el UUID del funcionario que está tomando la decisión.
                infraccion.setIdUsuarioJpl(idAdministrativoJpl);

                // Guardar los cambios
                Infraccion infraccionActualizada = infraccionRepository.save(infraccion);

                log.info("Infracción ID: {} procesada exitosamente con estado: {}", idInfraccion,
                                infraccionActualizada.getEstado());

                return InfraccionResponse.fromEntity(infraccionActualizada);
        }

        @Transactional
        public List<InfraccionResponse> findByDate(LocalDate date) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

                return infraccionRepository
                        .findByFechaBetween(startOfDay, endOfDay)
                        .stream()
                        .map(InfraccionResponse::fromEntity)
                        .toList();
        }

}