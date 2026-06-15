package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.TipoInfraccionDTO;
import com.sifa.core_sifa.exception.ResourceNotFoundException;
import com.sifa.core_sifa.model.TipoInfraccion;
import com.sifa.core_sifa.repository.ITipoInfraccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TipoInfraccionServiceImplTest {

    @Mock
    private ITipoInfraccionRepository tipoInfraccionRepository;

    @InjectMocks
    private TipoInfraccionServiceImpl tipoInfraccionService;

    private TipoInfraccion createTipoInfraccion(Integer id, String nombre, boolean habilitado) {
        return TipoInfraccion.builder()
                .idTipoInfraccion(id)
                .nombre(nombre)
                .disposicionInfringida("Art. " + id)
                .habilitado(habilitado)
                .build();
    }

    @Test
    void findAll_returnsOnlyHabilitados() {
        var tipo1 = createTipoInfraccion(1, "Tipo A", true);
        var tipo2 = createTipoInfraccion(2, "Tipo B", false);
        given(tipoInfraccionRepository.findAll()).willReturn(List.of(tipo1, tipo2));

        var result = tipoInfraccionService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNombre()).isEqualTo("Tipo A");
    }

    @Test
    void findAllPaged_returnsPagedResult() {
        var tipo = createTipoInfraccion(1, "Tipo A", true);
        var page = new PageImpl<>(List.of(tipo));
        given(tipoInfraccionRepository.findByHabilitadoTrue(PageRequest.of(0, 10)))
                .willReturn(page);

        Page<TipoInfraccionDTO> result = tipoInfraccionService.findAllPaged(PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_whenExists_returnsTipo() {
        var tipo = createTipoInfraccion(1, "Mal Estacionado", true);
        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(tipo));

        var result = tipoInfraccionService.findById(1);

        assertThat(result.getNombre()).isEqualTo("Mal Estacionado");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        given(tipoInfraccionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tipoInfraccionService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tipo de infracción no encontrado");
    }

    @Test
    void create_savesAndReturnsDTO() {
        var dto = TipoInfraccionDTO.builder()
                .nombre("Nuevo Tipo")
                .disposicionInfringida("Art. 999")
                .build();
        var saved = createTipoInfraccion(1, "Nuevo Tipo", true);
        given(tipoInfraccionRepository.save(any(TipoInfraccion.class))).willReturn(saved);

        var result = tipoInfraccionService.create(dto);

        assertThat(result.getNombre()).isEqualTo("Nuevo Tipo");
    }

    @Test
    void update_whenExists_updatesAndReturns() {
        var existing = createTipoInfraccion(1, "Viejo", true);
        var dto = TipoInfraccionDTO.builder()
                .nombre("Actualizado")
                .disposicionInfringida("Art. 999")
                .build();

        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(existing));
        given(tipoInfraccionRepository.save(any(TipoInfraccion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        var result = tipoInfraccionService.update(1, dto);

        assertThat(result.getNombre()).isEqualTo("Actualizado");
    }

    @Test
    void delete_disablesTipoInfraccion() {
        var existing = createTipoInfraccion(1, "Tipo", true);
        given(tipoInfraccionRepository.findById(1)).willReturn(Optional.of(existing));
        given(tipoInfraccionRepository.save(any(TipoInfraccion.class)))
                .willAnswer(inv -> inv.getArgument(0));

        tipoInfraccionService.delete(1);

        assertThat(existing.getHabilitado()).isFalse();
        verify(tipoInfraccionRepository).save(existing);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        given(tipoInfraccionRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tipoInfraccionService.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
