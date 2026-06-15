package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.Citacion;
import com.sifa.core_sifa.model.Infraccion;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.model.Vehiculo;
import com.sifa.core_sifa.repository.ICitacionRepository;
import com.sifa.core_sifa.repository.IInfraccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CitacionServiceTest {

    @Mock
    private ICitacionRepository citacionRepository;

    @Mock
    private IInfraccionRepository infraccionRepository;

    @InjectMocks
    private CitacionService citacionService;

    private Infraccion createInfraccion() {
        return Infraccion.builder()
                .idInfraccion(1)
                .idFiscalizador("fiscalizador@test.cl")
                .lugar("Av. Test")
                .latitud(-33.0f).longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .vehiculo(Vehiculo.builder().patente("ABCD12").build())
                .tipoInfraccion(TipoInfraccion.builder().idTipoInfraccion(1).nombre("Test").build())
                .build();
    }

    @Test
    void findAll_withFilters_returnsPagedResults() {
        var infraccion = createInfraccion();
        var citacion = Citacion.builder()
                .idCitacion(1)
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build();
        var page = new PageImpl<>(List.of(citacion));

        given(citacionRepository.findByFilters(any(), any(), any(), any()))
                .willReturn(page);

        var result = citacionService.findAll(null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getIdCitacion()).isEqualTo(1);
    }

    @Test
    void findById_whenExists_returnsCitacion() {
        var infraccion = createInfraccion();
        var citacion = Citacion.builder()
                .idCitacion(1)
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build();

        given(citacionRepository.findById(1)).willReturn(Optional.of(citacion));

        var result = citacionService.findById(1);

        assertThat(result.getIdCitacion()).isEqualTo(1);
    }

    @Test
    void findById_whenNotFound_throwsException() {
        given(citacionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> citacionService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Citación no encontrada");
    }

    @Test
    void crearCitacion_withValidData_createsSuccessfully() {
        var infraccion = createInfraccion();
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));
        given(citacionRepository.save(any(Citacion.class)))
                .willAnswer(inv -> {
                    Citacion saved = inv.getArgument(0);
                    saved.setIdCitacion(1);
                    return saved;
                });

        var result = citacionService.crearCitacion(1, LocalDateTime.now().plusDays(7));

        assertThat(result).isNotNull();
        assertThat(result.getIdCitacion()).isEqualTo(1);
    }

    @Test
    void crearCitacion_whenInfraccionNotFound_throwsException() {
        given(infraccionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> citacionService.crearCitacion(99, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Infracción no encontrada");
    }

    @Test
    void crearCitacion_whenAlreadyHasCitacion_throwsException() {
        var infraccion = createInfraccion();
        infraccion.setCitacion(Citacion.builder().idCitacion(5).build());
        given(infraccionRepository.findById(1)).willReturn(Optional.of(infraccion));

        assertThatThrownBy(() -> citacionService.crearCitacion(1, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya tiene una citación");
    }

    @Test
    void actualizarCitacion_withValidData_updatesSuccessfully() {
        var infraccion = createInfraccion();
        var citacion = Citacion.builder()
                .idCitacion(1)
                .fecha(LocalDateTime.now().plusDays(7))
                .infraccion(infraccion)
                .build();
        var request = CitacionUpdateRequest.builder()
                .fecha(LocalDateTime.now().plusDays(14))
                .build();

        given(citacionRepository.findById(1)).willReturn(Optional.of(citacion));
        given(citacionRepository.save(any(Citacion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var result = citacionService.actualizarCitacion(1, request, "jpl@test.cl");

        assertThat(result).isNotNull();
        verify(citacionRepository).save(any(Citacion.class));
    }

    @Test
    void actualizarCitacion_whenNotFound_throwsException() {
        var request = CitacionUpdateRequest.builder()
                .fecha(LocalDateTime.now().plusDays(14))
                .build();

        given(citacionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> citacionService.actualizarCitacion(99, request, "jpl@test.cl"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Citación no encontrada");
    }
}
