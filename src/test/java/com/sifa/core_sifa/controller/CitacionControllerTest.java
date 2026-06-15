package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.citacion.CitacionResponse;
import com.sifa.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.CitacionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CitacionController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class CitacionControllerTest extends ControllerTestBase {

    @MockitoBean
    private CitacionService citacionService;

    @Test
    void getAllCitaciones_returnsOk() throws Exception {
        var citacion = CitacionResponse.builder()
                .idCitacion(1)
                .fecha(LocalDateTime.now().plusDays(7))
                .build();
        var page = new PageImpl<>(List.of(citacion));

        given(citacionService.findAll(any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/citaciones/all")
                        .headers(authHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idCitacion").value(1));
    }

    @Test
    void getCitacionById_whenExists_returnsOk() throws Exception {
        var citacion = CitacionResponse.builder()
                .idCitacion(1)
                .fecha(LocalDateTime.now().plusDays(7))
                .build();

        given(citacionService.findById(1)).willReturn(citacion);

        mockMvc.perform(get("/core/api/v1/citaciones/1")
                        .headers(authHeaders("USER_JPL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCitacion").value(1));
    }

    @Test
    void actualizarCitacion_returnsOk() throws Exception {
        var request = CitacionUpdateRequest.builder()
                .fecha(LocalDateTime.now().plusDays(14))
                .build();

        given(citacionService.actualizarCitacion(any(), any(), any()))
                .willReturn(CitacionResponse.builder().idCitacion(1).build());

        mockMvc.perform(put("/core/api/v1/citaciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .headers(authHeaders("USER_JPL")))
                .andExpect(status().isOk());
    }
}
