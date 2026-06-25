package com.sifa.core_sifa.service.infraccion;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import com.sifa.core_sifa.repository.IInfraccionRepository;
import com.sifa.core_sifa.repository.ITipoInfraccionRepository;
import com.sifa.core_sifa.repository.IVehiculoRepository;
import com.sifa.core_sifa.service.CitacionService;
import com.sifa.core_sifa.service.IStorageService;
import com.sifa.core_sifa.service.audits.IAuditLogService;
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
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfraccionServiceImplTest {

    @Mock
    private IInfraccionRepository infraccionRepository;

    @Mock
    private IVehiculoRepository vehiculoRepository;

    @Mock
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @Mock
    private IStorageService storageService;

    @Mock
    private CitacionService citacionService;

    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private InfraccionServiceImpl infraccionService;

    @Captor
    private ArgumentCaptor<Infraccion> infraccionCaptor;

    private Vehiculo createVehiculo() {
        return Vehiculo.builder()
                .patente("ABCD12")
                .marca("TOYOTA").modelo("YARIS")
                .anioFabricacion(2020).color("BLANCO")
                .nroMotor("M123").nroSerie("S456")
                .build();
    }

    private TipoInfraccion createTipoInfraccion() {
        return TipoInfraccion.builder()
                .idTipoInfraccion(1)
                .nombre("Mal Estacionado")
                .disposicionInfringida("Art. 154")
                .build();
    }

    private Infraccion createInfraccion(Integer id, String estado) {
        var vehiculo = createVehiculo();
        var tipo = createTipoInfraccion();
        return Infraccion.builder()
                .idInfraccion(id)
                .idFiscalizador("fiscalizador@test.cl")
                .lugar("Av. Test")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado(estado)
                .vehiculo(vehiculo)
                .tipoInfraccion(tipo)
                .build();
    }

    @Test
    void findAllInfracciones_returnsAllSortedByFechaDesc() {
        var infracciones = List.of(
                createInfraccion(1, "EN PROCESO"),
                createInfraccion(2, "APROBADA")
        );
        given(infraccionRepository.findAll(Sort.by(Sort.Direction.DESC, "fecha")))
                .willReturn(infracciones);

        var result = infraccionService.findAllInfracciones();

        assertThat(result).hasSize(2);
    }

    @Test
    void findById_whenExists_returnsInfraccion() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        var result = infraccionService.findById(1);

        assertThat(result.getId()).isEqualTo("1");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        given(infraccionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> infraccionService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Infraccion no encontrada");
    }

    @Test
    void findByIdFiscalizador_returnsPagedResults() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        var page = new PageImpl<>(List.of(infraccion));
        given(infraccionRepository.findByIdFiscalizadorOrderByFechaDesc(
                "fiscalizador@test.cl", PageRequest.of(0, 10)))
                .willReturn(page);

        var result = infraccionService.findByIdFiscalizador(
                "fiscalizador@test.cl", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findByVehiculoPatente_returnsInfracciones() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        given(infraccionRepository.findByVehiculoPatenteOrderByFechaDesc("ABCD12"))
                .willReturn(List.of(infraccion));

        var result = infraccionService.findByVehiculoPatente("ABCD12");

        assertThat(result).hasSize(1);
    }

    @Test
    void crearInfraccion_withValidData_createsSuccessfully() {
        var vehiculo = createVehiculo();
        var tipo = createTipoInfraccion();
        var request = InfraccionCreateRequest.builder()
                .patenteVehiculo("ABCD12")
                .idTipoInfraccion(1)
                .lugar("Av. Test")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .fechaCitacion(LocalDateTime.now().plusDays(7))
                .observaciones("Test")
                .build();
        var foto = mock(MultipartFile.class);
        var fotos = List.of(foto);

        given(vehiculoRepository.findById("ABCD12")).willReturn(Optional.of(vehiculo));
        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(tipo));
        given(storageService.uploadFiles(fotos, "ABCD12"))
                .willReturn(List.of("https://test.com/foto.jpg"));
        given(infraccionRepository.save(any(Infraccion.class)))
                .willAnswer(invocation -> {
                    Infraccion saved = invocation.getArgument(0);
                    saved.setIdInfraccion(1);
                    return saved;
                });

        var result = infraccionService.crearInfraccion(request, fotos, "fiscalizador@test.cl");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("1");
        verify(citacionService).crearCitacion(eq(1), any(LocalDateTime.class));
    }

    @Test
    void crearInfraccion_withoutPhotos_throwsException() {
        var vehiculo = createVehiculo();
        var tipo = createTipoInfraccion();
        var request = InfraccionCreateRequest.builder()
                .patenteVehiculo("ABCD12")
                .idTipoInfraccion(1)
                .lugar("Av. Test")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .fechaCitacion(LocalDateTime.now().plusDays(7))
                .build();

        given(vehiculoRepository.findById("ABCD12")).willReturn(Optional.of(vehiculo));
        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(tipo));

        assertThatThrownBy(() -> infraccionService.crearInfraccion(request, null, "fiscalizador@test.cl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una fotografía");
    }

    @Test
    void crearInfraccion_withEmptyPhotos_throwsException() {
        var vehiculo = createVehiculo();
        var tipo = createTipoInfraccion();
        var request = InfraccionCreateRequest.builder()
                .patenteVehiculo("ABCD12")
                .idTipoInfraccion(1)
                .lugar("Av. Test")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .fechaCitacion(LocalDateTime.now().plusDays(7))
                .build();

        given(vehiculoRepository.findById("ABCD12")).willReturn(Optional.of(vehiculo));
        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(tipo));

        assertThatThrownBy(() -> infraccionService.crearInfraccion(request, List.of(), "fiscalizador@test.cl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una fotografía");
    }

    @Test
    void procesarInfraccionPorJpl_withValidData_processesSuccessfully() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        var request = InfraccionUpdateRequest.builder()
                .estado("APROBADA")
                .build();

        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));
        given(infraccionRepository.save(any(Infraccion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var result = infraccionService.procesarInfraccionPorJpl(1, request, "jpl@test.cl");

        assertThat(result.getStatus()).isEqualTo("accepted");
        verify(auditLogService).registrarLog(any(AuditLogRequestDTO.class));
    }

    @Test
    void procesarInfraccionPorJpl_withAlreadyProcessed_throwsException() {
        var infraccion = createInfraccion(1, "EXPORTADA");
        var request = InfraccionUpdateRequest.builder().estado("RECHAZADA").build();

        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.procesarInfraccionPorJpl(1, request, "jpl@test.cl"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPORTADA");
    }

    @Test
    void procesarInfraccionPorJpl_withRejectedNoMotivo_throwsException() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        var request = InfraccionUpdateRequest.builder()
                .estado("RECHAZADA")
                .motivoRechazo("")
                .build();

        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.procesarInfraccionPorJpl(1, request, "jpl@test.cl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo de rechazo");
    }

    @Test
    void findInfracciones_withFilters_callsRepository() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        var page = new PageImpl<>(List.of(infraccion));
        given(infraccionRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .willReturn(page);

        var result = infraccionService.findInfracciones(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "fiscalizador@test.cl",
                "pending",
                null,
                PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void actualizarEstadoInfraccion_withValidStatus_updatesCorrectly() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));
        given(infraccionRepository.save(any(Infraccion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var result = infraccionService.actualizarEstadoInfraccion(1, "accepted", "jpl@test.cl", null);

        assertThat(result.getStatus()).isEqualTo("accepted");
        verify(auditLogService).registrarLog(any(AuditLogRequestDTO.class));
    }

    @Test
    void actualizarEstadoInfraccion_whenRechazada_throwsIllegalState() {
        var infraccion = createInfraccion(1, "RECHAZADA");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.actualizarEstadoInfraccion(1, "accepted", "jpl@test.cl", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RECHAZADA");
    }

    @Test
    void actualizarEstadoInfraccion_whenExportada_throwsIllegalState() {
        var infraccion = createInfraccion(1, "EXPORTADA");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.actualizarEstadoInfraccion(1, "accepted", "jpl@test.cl", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPORTADA");
    }

    @Test
    void actualizarEstadoInfraccion_whenRejectedWithoutMotivo_throwsIllegalArgument() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.actualizarEstadoInfraccion(1, "rejected", "jpl@test.cl", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo de rechazo");
    }

    @Test
    void editarInfraccion_updatesFieldsCorrectly() {
        var infraccion = createInfraccion(1, "EN PROCESO");
        java.util.Map<String, Object> updates = Map.of(
                "observaciones", "Nueva observacion",
                "status", "accepted"
        );

        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));
        given(infraccionRepository.save(any(Infraccion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var result = infraccionService.editarInfraccion(1, updates);

        assertThat(result.getObservaciones()).isEqualTo("Nueva observacion");
        verify(infraccionRepository).save(infraccionCaptor.capture());
        assertThat(infraccionCaptor.getValue().getEstado()).isEqualTo("APROBADA");
    }

    @Test
    void editarInfraccion_whenRechazada_throwsIllegalState() {
        var infraccion = createInfraccion(1, "RECHAZADA");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.editarInfraccion(1, Map.of("status", "accepted")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RECHAZADA");
    }

    @Test
    void editarInfraccion_whenExportada_throwsIllegalState() {
        var infraccion = createInfraccion(1, "EXPORTADA");
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> infraccionService.editarInfraccion(1, Map.of("status", "pending")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPORTADA");
    }

    @Test
    void findCoordenadas_returnsCoordinates() {
        given(infraccionRepository.findCoordenadasByFilters(any(), any(), any()))
                .willReturn(List.of());

        var result = infraccionService.findCoordenadas(null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void obtenerProductividad_withValidDates_returnsProductividad() {
        given(infraccionRepository.countProductividadPorFiscalizador(any(), any()))
                .willReturn(List.of());

        var result = infraccionService.obtenerProductividad(
                LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    void obtenerProductividad_withInvertedDates_throwsException() {
        assertThatThrownBy(() -> infraccionService.obtenerProductividad(
                LocalDate.now(), LocalDate.now().minusDays(7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser posterior");
    }

    @Test
    void obtenerEstadisticasDashboard_withInvertedDates_throwsException() {
        assertThatThrownBy(() -> infraccionService.obtenerEstadisticasDashboard(
                LocalDate.now(), LocalDate.now().minusDays(1), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser posterior");
    }
}
