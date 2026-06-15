package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.model.FiscalizadorPresencia;
import com.sifa.core_sifa.service.FiscalizadorPresenciaService;
import com.sifa.core_sifa.service.device.IDeviceTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FiscalizadorPresenciaController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class FiscalizadorPresenciaControllerTest extends ControllerTestBase {

    @MockitoBean
    private FiscalizadorPresenciaService presenciaService;

    @MockitoBean
    private IDeviceTokenService deviceTokenService;

    @Test
    void recibirHeartbeat_returnsOk() throws Exception {
        var request = """
                {
                    "latitud": -33.0,
                    "longitud": -71.0,
                    "deviceId": "device-123",
                    "marca": "Google",
                    "modelo": "Pixel 7"
                }
                """;

        mockMvc.perform(post("/core/api/v1/fis-activity/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "fiscalizador@test.cl")
                        .header("X-Auth-Roles", "USER_APP"))
                .andExpect(status().isOk());

        verify(presenciaService).registrarLatido(eq("fiscalizador@test.cl"), any(FiscalizadorHeartbeatRequest.class));
        verify(deviceTokenService).processHeartbeat(eq("fiscalizador@test.cl"), any(FiscalizadorHeartbeatRequest.class));
    }

    @Test
    void recibirHeartbeat_sinRoleApp_retornaForbidden() throws Exception {
        var request = """
                {
                    "latitud": -33.0,
                    "longitud": -71.0,
                    "deviceId": "device-123",
                    "marca": "Google",
                    "modelo": "Pixel 7"
                }
                """;

        mockMvc.perform(post("/core/api/v1/fis-activity/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFiscalizadoresActivos_returnsPagedResults() throws Exception {
        var presencia = new FiscalizadorPresencia();
        var page = new PageImpl<>(List.of(presencia));
        given(presenciaService.obtenerFiscalizadoresActivos(any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/fis-activity/activos")
                        .headers(authHeaders("USER_SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
