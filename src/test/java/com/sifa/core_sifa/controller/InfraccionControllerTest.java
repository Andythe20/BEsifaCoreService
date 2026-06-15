package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.infraccion.DashboardEstadisticasDTO;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.ProductividadFiscalizadorDTO;
import com.sifa.core_sifa.dto.infraccion.ReporteResumenDTO;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.infraccion.IInfraccionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InfraccionController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class InfraccionControllerTest extends ControllerTestBase {

    @MockitoBean
    private IInfraccionService infraccionService;

    @Test
    void getAllInfracciones_returnsOk() throws Exception {
        var page = new PageImpl<>(List.of(createResponse()));
        given(infraccionService.findInfracciones(any(), any(), any(), any(), any(), any()))
                .willReturn(page);

        mockMvc.perform(get("/core/api/v1/infracciones/all")
                        .headers(authHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllInfracciones_withInvalidDateRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/core/api/v1/infracciones/all")
                        .param("startDate", "2024-01-10")
                        .param("endDate", "2024-01-01")
                        .headers(authHeaders()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInfraccionById_whenExists_returnsOk() throws Exception {
        given(infraccionService.findById(1)).willReturn(createResponse());

        mockMvc.perform(get("/core/api/v1/infracciones/id/1")
                        .headers(authHeaders("USER_JPL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void crearInfraccion_withValidMultipart_returnsCreated() throws Exception {
        var requestJson = """
                {
                    "patenteVehiculo": "ABCD12",
                    "idTipoInfraccion": 1,
                    "lugar": "Av. Test",
                    "latitud": -33.0,
                    "longitud": -71.0,
                    "fecha": "2024-06-15T14:30:00.000000",
                    "fechaCitacion": "2024-06-22T14:30:00.000000"
                }
                """;

        var foto = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", "test-image".getBytes());
        var infraccionPart = new MockMultipartFile(
                "infraccion", "infraccion", "application/json", requestJson.getBytes());

        given(infraccionService.crearInfraccion(any(), any(), eq("fiscalizador@test.cl")))
                .willReturn(createResponse());

        mockMvc.perform(multipart("/core/api/v1/infracciones")
                        .file(foto)
                        .file(infraccionPart)
                        .header("X-Auth-User", "fiscalizador@test.cl")
                        .header("X-Auth-Roles", "USER_APP"))
                .andExpect(status().isCreated());
    }

    @Test
    void procesarInfraccion_returnsOk() throws Exception {
        var requestJson = """
                {
                    "estado": "APROBADA"
                }
                """;

        given(infraccionService.procesarInfraccionPorJpl(eq(1), any(), eq("jpl@test.cl")))
                .willReturn(createResponse());

        mockMvc.perform(put("/core/api/v1/infracciones/1/procesar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("X-Auth-User", "jpl@test.cl")
                        .header("X-Auth-Roles", "USER_JPL"))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarEstado_returnsOk() throws Exception {
        var body = Map.of("status", "accepted");
        given(infraccionService.actualizarEstadoInfraccion(eq(1), eq("accepted"), any(), any()))
                .willReturn(createResponse());

        mockMvc.perform(patch("/core/api/v1/infracciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .headers(authHeaders("USER_JPL")))
                .andExpect(status().isOk());
    }

    @Test
    void getCoordenadas_returnsOk() throws Exception {
        given(infraccionService.findCoordenadas(any(), any(), any()))
                .willReturn(List.of());

        mockMvc.perform(get("/core/api/v1/infracciones/coordenadas")
                        .headers(authHeaders()))
                .andExpect(status().isOk());
    }

    @Test
    void getResumenReporte_returnsOk() throws Exception {
        given(infraccionService.obtenerResumenReporte(any(), any(), any()))
                .willReturn(ReporteResumenDTO.builder().build());

        mockMvc.perform(get("/core/api/v1/infracciones/resumen-reporte")
                        .headers(authHeaders()))
                .andExpect(status().isOk());
    }

    @Test
    void getProductividad_returnsOk() throws Exception {
        given(infraccionService.obtenerProductividad(any(), any()))
                .willReturn(List.of(new ProductividadFiscalizadorDTO("user@test.cl", 5L)));

        mockMvc.perform(get("/core/api/v1/infracciones/reporte/productividad")
                        .headers(authHeaders("USER_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    void getEstadisticasDashboard_returnsOk() throws Exception {
        given(infraccionService.obtenerEstadisticasDashboard(any(), any(), any(), any()))
                .willReturn(DashboardEstadisticasDTO.builder().totalInfracciones(10L).build());

        mockMvc.perform(get("/core/api/v1/infracciones/estadisticas")
                        .headers(authHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInfracciones").value(10));
    }

    private InfraccionResponse createResponse() {
        return InfraccionResponse.builder()
                .id("1")
                .idFiscalizador("fiscalizador@test.cl")
                .fecha(LocalDateTime.now())
                .status("pending")
                .build();
    }
}
