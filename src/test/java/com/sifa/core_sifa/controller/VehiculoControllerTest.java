package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.VehiculoDTO;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehiculoController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class VehiculoControllerTest extends ControllerTestBase {

    @MockitoBean
    private VehiculoService vehiculoService;

    @Test
    void findAllVehiculos_cuandoHayVehiculos_returnsList() throws Exception {
        var vehiculo = VehiculoDTO.builder().patente("ABCD12").marca("TOYOTA").build();
        given(vehiculoService.findAllVehiculos()).willReturn(List.of(vehiculo));

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patente").value("ABCD12"));
    }

    @Test
    void findAllVehiculos_cuandoVacio_returnsNoContent() throws Exception {
        given(vehiculoService.findAllVehiculos()).willReturn(List.of());

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void findById_returnsVehiculo() throws Exception {
        var vehiculo = VehiculoDTO.builder().patente("ABCD12").marca("TOYOTA").build();
        given(vehiculoService.findById("ABCD12")).willReturn(vehiculo);

        mockMvc.perform(get("/core/api/v1/vehiculos/id/ABCD12")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patente").value("ABCD12"));
    }
}
