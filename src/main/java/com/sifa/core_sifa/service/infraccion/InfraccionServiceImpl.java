package com.sifa.core_sifa.service.infraccion;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.EvidenciaFotografica;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.repository.IInfraccionRepository;
import com.sifa.core_sifa.repository.ITipoInfraccionRepository;
import com.sifa.core_sifa.repository.IVehiculoRepository;
import com.sifa.core_sifa.service.IStorageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio encargado de gestionar toda la lógica de negocio relacionada con las
 * Infracciones.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InfraccionServiceImpl implements IInfraccionService {

        private final IInfraccionRepository infraccionRepository;
        private final IVehiculoRepository vehiculoRepository;
        private final ITipoInfraccionRepository tipoInfraccionRepository;
        private final IStorageService storageService;

        @Override
        @Transactional(readOnly = true)
        public List<InfraccionResponse> findAllInfracciones() {
                log.info("Listando todas las infracciones");

                return infraccionRepository.findAll(
                                Sort.by(Sort.Direction.DESC, "fecha"))
                                .stream()
                                .map(InfraccionResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public InfraccionResponse findById(Integer idInfraccion) {
                log.info("Buscando infraccion con id: {}", idInfraccion);
                Infraccion infraccion = infraccionRepository.findById(idInfraccion)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infraccion no encontrada o inexistente"));

                return InfraccionResponse.fromEntity(infraccion);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<InfraccionResponse> findByIdFiscalizador(
                        String idFiscalizador,
                        Pageable pageable) {

                log.info("Buscando infracciones por id Fiscalizador: {}", idFiscalizador);

                Page<Infraccion> listaInfracciones = infraccionRepository.findByIdFiscalizadorOrderByFechaDesc(
                                idFiscalizador,
                                pageable);

                return listaInfracciones.map(InfraccionResponse::fromEntity);
        }

        @Override
        @Transactional(readOnly = true)
        public List<InfraccionResponse> findByVehiculoPatente(String vehiculoPatente) {
                log.info("Buscando infracciones por patente: {}", vehiculoPatente);

                List<Infraccion> listaInfracciones = infraccionRepository
                                .findByVehiculoPatenteOrderByFechaDesc(vehiculoPatente);

                return listaInfracciones.stream()
                                .map(InfraccionResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Registra una nueva infracción cursada por un fiscalizador desde la App Móvil.
         * 
         * Lógica clave:
         * 1. Valida existencia de Patente y Tipo de Infracción.
         * 2. Obliga a adjuntar al menos una fotografía de evidencia.
         * 3. Sube los archivos al storage configurado (Mock local o S3 en producción).
         * 4. Si la inserción en BD falla, hace ROLLBACK físico eliminando los archivos
         * subidos al storage.
         */
        @Override
        @Transactional
        public InfraccionResponse crearInfraccion(InfraccionCreateRequest request, List<MultipartFile> fotos,
                        String idFiscalizador) {
                log.info("Iniciando creación de infracción para patente: {}", request.getPatenteVehiculo());

                var vehiculo = vehiculoRepository.findById(request.getPatenteVehiculo())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vehículo no encontrado con patente: " + request.getPatenteVehiculo()));

                var tipoInfraccion = tipoInfraccionRepository.findById(request.getIdTipoInfraccion())
                                .orElseThrow(() -> new IllegalArgumentException("Tipo de infracción no válido"));

                Infraccion nuevaInfraccion = Infraccion.builder()
                                .idFiscalizador(idFiscalizador)
                                .idUsuarioJpl(null)
                                .lugar(request.getLugar())
                                .latitud(request.getLatitud())
                                .longitud(request.getLongitud())
                                .observaciones(request.getObservaciones())
                                .fecha(request.getFecha())
                                .estado("EN PROCESO")
                                .vehiculo(vehiculo)
                                .tipoInfraccion(tipoInfraccion)
                                .build();

                if (fotos == null || fotos.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Es obligatorio adjuntar al menos una fotografía de evidencia.");
                }

                List<String> uploadedUrls = new java.util.ArrayList<>();

                try {
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

                        Infraccion infraccionGuardada = infraccionRepository.save(nuevaInfraccion);

                        log.info("Infracción creada exitosamente con ID: {}",
                                        infraccionGuardada.getIdInfraccion());

                        return InfraccionResponse.fromEntity(infraccionGuardada);

                } catch (Exception e) {
                        log.error("Error creando infracción. Rollback archivos storage", e);

                        // Si el guardado en BD falla, eliminamos los archivos para evitar "basura" en
                        // S3
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

        /**
         * Procesa o decide el estado final de una infracción por parte del JPL.
         * 
         * Lógica clave:
         * 1. Bloquea el reprocesamiento de infracciones ya cerradas (Aprobadas o
         * Rechazadas).
         * 2. Obliga a ingresar un motivo de rechazo si el estado asignado es
         * 'RECHAZADA'.
         * 3. Registra la fecha de la resolución y el UUID del funcionario JPL.
         */
        @Override
        @Transactional
        public InfraccionResponse procesarInfraccionPorJpl(Integer idInfraccion, InfraccionUpdateRequest request,
                        String idAdministrativoJpl) {
                log.info("Administrativo JPL {} procesando infracción ID: {}", idAdministrativoJpl, idInfraccion);

                Infraccion infraccion = infraccionRepository.findById(idInfraccion)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infracción no encontrada con ID: " + idInfraccion));

                if ("APROBADA".equalsIgnoreCase(infraccion.getEstado())
                                || "RECHAZADA".equalsIgnoreCase(infraccion.getEstado())) {
                        throw new IllegalStateException(
                                        "La infracción ya fue procesada anteriormente y no puede ser modificada.");
                }

                if ("RECHAZADA".equalsIgnoreCase(request.getEstado()) &&
                                (request.getMotivoRechazo() == null || request.getMotivoRechazo().trim().isEmpty())) {
                        throw new IllegalArgumentException("Debe ingresar un motivo de rechazo válido.");
                }

                infraccion.setEstado(request.getEstado().toUpperCase());
                infraccion.setMotivoRechazo(request.getMotivoRechazo());
                infraccion.setFechaResolucion(java.time.LocalDateTime.now());
                infraccion.setIdUsuarioJpl(idAdministrativoJpl);

                Infraccion infraccionActualizada = infraccionRepository.save(infraccion);

                log.info("Infracción ID: {} procesada exitosamente con estado: {}", idInfraccion,
                                infraccionActualizada.getEstado());

                return InfraccionResponse.fromEntity(infraccionActualizada);
        }

        @Override
        @Transactional
        public List<InfraccionResponse> findByDate(LocalDate date) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

                return infraccionRepository
                                .findByFechaBetweenOrderByFechaDesc(startOfDay, endOfDay)
                                .stream()
                                .map(InfraccionResponse::fromEntity)
                                .toList();
        }

        /**
         * Búsqueda avanzada con filtros combinados (Fecha y/o Fiscalizador).
         */
        @Override
        @Transactional(readOnly = true)
        public Page<InfraccionResponse> findInfracciones(
                        LocalDate startDate,
                        LocalDate endDate,
                        String user,
                        Pageable pageable) {

                LocalDateTime start = null;
                LocalDateTime end = null;

                if (startDate != null) {
                        start = startDate.atStartOfDay();
                }
                if (endDate != null) {
                        end = endDate.atTime(23, 59, 59);
                }

                Page<Infraccion> infracciones = infraccionRepository.findByFilters(
                                start,
                                end,
                                user,
                                pageable);

                return infracciones.map(InfraccionResponse::fromEntity);
        }

        /**
         * Cambia el estado de una infracción y mapea la nomenclatura del Frontend
         * (accepted, rejected)
         * a los estados de la base de datos (APROBADA, RECHAZADA).
         */
        @Override
        @Transactional
        public InfraccionResponse actualizarEstadoInfraccion(Integer id, String status, String idUsuario) {
                Infraccion infraccion = infraccionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infracción no encontrada con ID: " + id));

                String dbStatus = status == null ? "EN PROCESO" : switch (status.toLowerCase()) {
                        case "pending" -> "EN PROCESO";
                        case "accepted" -> "APROBADA";
                        case "rejected" -> "RECHAZADA";
                        case "exported" -> "EXPORTADA";
                        default -> status.toUpperCase();
                };

                infraccion.setEstado(dbStatus);
                if (idUsuario != null) {
                        infraccion.setIdUsuarioJpl(idUsuario);
                }
                infraccion.setFechaResolucion(java.time.LocalDateTime.now());

                return InfraccionResponse.fromEntity(infraccionRepository.save(infraccion));
        }

        /**
         * Permite la edición parcial y dinámica de los campos de la infracción desde el
         * Dashboard.
         * 
         * Lógica clave:
         * 1. Mapea campos planos (boleta, parte, observaciones, estado).
         * 2. Si se cambia 'infractionCode', busca y actualiza de forma segura la
         * relación de tipo de infracción.
         * 3. Si se cambia la patente en 'vehicle', vincula el nuevo registro del
         * vehículo.
         * 4. Si se cambia la 'fechaCitacion', crea o actualiza la entidad Citacion
         * aplicando formato seguro.
         */
        @Override
        @Transactional
        public InfraccionResponse editarInfraccion(Integer id, java.util.Map<String, Object> request) {
                log.info("Editando infracción ID: {}", id);

                Infraccion infraccion = infraccionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Infracción no encontrada con ID: " + id));

                if (request.containsKey("observaciones")) {
                        infraccion.setObservaciones((String) request.get("observaciones"));
                }
                if (request.containsKey("infractionDescription")) {
                        infraccion.setObservaciones((String) request.get("infractionDescription"));
                }

                if (request.containsKey("status")) {
                        String status = (String) request.get("status");
                        String dbStatus = status == null ? "EN PROCESO" : switch (status.toLowerCase()) {
                                case "pending" -> "EN PROCESO";
                                case "accepted" -> "APROBADA";
                                case "rejected" -> "RECHAZADA";
                                case "exported" -> "EXPORTADA";
                                default -> status.toUpperCase();
                        };
                        infraccion.setEstado(dbStatus);
                }

                if (request.containsKey("infractionCode")) {
                        try {
                                Integer code = Integer.parseInt(String.valueOf(request.get("infractionCode")));
                                var tipo = tipoInfraccionRepository.findById(code).orElse(null);
                                if (tipo != null) {
                                        infraccion.setTipoInfraccion(tipo);
                                }
                        } catch (Exception e) {
                                log.warn("Código de infracción inválido en edición: {}", request.get("infractionCode"));
                        }
                }

                if (request.containsKey("vehicle")) {
                        var vehicleMap = (java.util.Map<String, Object>) request.get("vehicle");
                        if (vehicleMap != null && vehicleMap.containsKey("plate")) {
                                String plate = (String) vehicleMap.get("plate");
                                if (plate != null && !plate.trim().isEmpty()) {
                                        var vehiculo = vehiculoRepository.findById(plate).orElse(null);
                                        if (vehiculo != null) {
                                                infraccion.setVehiculo(vehiculo);
                                        }
                                }
                        }
                }

                if (request.containsKey("tramitacion")) {
                        var tramMap = (java.util.Map<String, Object>) request.get("tramitacion");
                        if (tramMap != null && tramMap.containsKey("fechaCitacion")) {
                                String fechaStr = (String) tramMap.get("fechaCitacion");
                                if (fechaStr != null && !fechaStr.trim().isEmpty()
                                                && !"No definida".equalsIgnoreCase(fechaStr)) {
                                        try {
                                                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                                                                .ofPattern("yyyy-MM-dd HH:mm");
                                                java.time.LocalDateTime fCit = java.time.LocalDateTime.parse(fechaStr,
                                                                dtf);
                                                if (infraccion.getCitacion() != null) {
                                                        infraccion.getCitacion().setFecha(fCit);
                                                } else {
                                                        var cit = com.sifa.core_sifa.model.Citacion.builder()
                                                                        .fecha(fCit)
                                                                        .infraccion(infraccion)
                                                                        .build();
                                                        infraccion.setCitacion(cit);
                                                }
                                        } catch (Exception e) {
                                                log.warn("Error parseando fechaCitacion: {}", fechaStr);
                                        }
                                }
                        }
                }

                return InfraccionResponse.fromEntity(infraccionRepository.save(infraccion));
        }
}