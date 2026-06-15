package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.TipoInfraccionDTO;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.TipoInfraccionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TipoInfraccionController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class TipoInfraccionControllerTest extends ControllerTestBase {

    @MockitoBean
    private TipoInfraccionServiceImpl tipoInfraccionService;

    @Test
    void getAllTipoInfracciones_returnsOk() throws Exception {
        Page<TipoInfraccionDTO> page = new PageImpl<>(
                List.of(TipoInfraccionDTO.builder().id(1).nombre("Test").build()));

        given(tipoInfraccionService.findAllPaged(any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/tipoInfracciones/all")
                        .headers(authHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Test"));
    }

    @Test
    void getTipoInfraccionById_returnsOk() throws Exception {
        given(tipoInfraccionService.findById(1))
                .willReturn(TipoInfraccionDTO.builder().id(1).nombre("Mal Estacionado").build());

        mockMvc.perform(get("/core/api/v1/tipoInfracciones/id/1")
                        .headers(authHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mal Estacionado"));
    }

    @Test
    void createTipoInfraccion_returnsOk() throws Exception {
        var dto = TipoInfraccionDTO.builder().nombre("Nuevo Tipo").disposicionInfringida("Art. 1").build();

        given(tipoInfraccionService.create(any()))
                .willReturn(TipoInfraccionDTO.builder().id(1).nombre("Nuevo Tipo").build());

        mockMvc.perform(post("/core/api/v1/tipoInfracciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .headers(authHeaders()))
                .andExpect(status().isOk());
    }

    @Test
    void updateTipoInfraccion_returnsOk() throws Exception {
        var dto = TipoInfraccionDTO.builder().nombre("Actualizado").disposicionInfringida("Art. 2").build();

        given(tipoInfraccionService.update(eq(1), any()))
                .willReturn(TipoInfraccionDTO.builder().id(1).nombre("Actualizado").build());

        mockMvc.perform(put("/core/api/v1/tipoInfracciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .headers(authHeaders()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTipoInfraccion_returnsNoContent() throws Exception {
        doNothing().when(tipoInfraccionService).delete(1);

        mockMvc.perform(delete("/core/api/v1/tipoInfracciones/1")
                        .headers(authHeaders()))
                .andExpect(status().isNoContent());
    }
}
