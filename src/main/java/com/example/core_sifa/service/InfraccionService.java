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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfraccionService {

        private final IInfraccionRepository infraccionRepository;
        private final IVehiculoRepository vehiculoRepository;
        private final ITipoInfraccionRepository tipoInfraccionRepository;
        private final IStorageService s3Service;

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
        public InfraccionResponse crearInfraccion(
                        InfraccionCreateRequest request,
                        List<MultipartFile> fotos,
                        String idFiscalizador) {

                log.info("Iniciando creación de infracción para patente: {}",
                                request.getPatenteVehiculo());

                // Validar que el vehículo exista
                var vehiculo = vehiculoRepository.findById(request.getPatenteVehiculo())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vehículo no encontrado con patente: "
                                                                + request.getPatenteVehiculo()));

                // Validar que el tipo de infracción sea correcto
                var tipoInfraccion = tipoInfraccionRepository.findById(request.getIdTipoInfraccion())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Tipo de infracción no válido"));

                // Construir la entidad base
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

                // Lista auxiliar para rollback manual de S3
                List<String> uploadedUrls = new ArrayList<>();

                try {

                        List<EvidenciaFotografica> evidencias = new ArrayList<>();

                        // Procesar imágenes manualmente
                        for (int i = 0; i < fotos.size(); i++) {

                                MultipartFile foto = fotos.get(i);

                                // Nombre limpio:
                                // ABCD11_1.jpg
                                // ABCD11_2.jpg
                                String nombreArchivo = request.getPatenteVehiculo()
                                                + "_"
                                                + (i + 1);

                                // Subir archivo a S3
                                String urlS3 = s3Service.uploadFile(
                                                foto,
                                                nombreArchivo);

                                // Guardar URL por si hay rollback
                                uploadedUrls.add(urlS3);

                                // Crear evidencia
                                EvidenciaFotografica evidencia = EvidenciaFotografica.builder()
                                                .url(urlS3)
                                                .infraccion(nuevaInfraccion)
                                                .build();

                                evidencias.add(evidencia);
                        }

                        nuevaInfraccion.setEvidenciasFotograficas(evidencias);

                        // Guardar en BD
                        Infraccion infraccionGuardada = infraccionRepository.save(nuevaInfraccion);

                        log.info("Infracción creada exitosamente con ID: {}",
                                        infraccionGuardada.getIdInfraccion());

                        return InfraccionResponse.fromEntity(infraccionGuardada);

                } catch (Exception e) {

                        log.error(
                                        "Error creando infracción. Eliminando archivos subidos de S3",
                                        e);

                        // Eliminar archivos subidos
                        uploadedUrls.forEach(url -> {
                                try {
                                        s3Service.deleteFile(url);
                                } catch (Exception deleteException) {

                                        log.error(
                                                        "Error eliminando archivo S3: {}",
                                                        url,
                                                        deleteException);
                                }
                        });

                        // Relanzar excepción para rollback de BD
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

}
