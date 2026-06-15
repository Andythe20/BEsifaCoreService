package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.device.IDeviceTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceTokenController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class DeviceTokenControllerTest extends ControllerTestBase {

    @MockitoBean
    private IDeviceTokenService deviceTokenService;

    @Test
    void registerDevice_returnsOk() throws Exception {
        var request = """
                {
                    "token": "fcm-token-123",
                    "platform": "ANDROID",
                    "appVersion": "1.0.0"
                }
                """;

        mockMvc.perform(post("/core/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "fiscalizador@test.cl")
                        .header("X-Auth-Roles", "USER_APP"))
                .andExpect(status().isOk());
    }

    @Test
    void listDevices_returnsList() throws Exception {
        var device = DeviceTokenResponse.builder()
                .id(1L).emailUsuario("user@test.cl").platform("ANDROID")
                .build();
        given(deviceTokenService.getAllDeviceResponses()).willReturn(List.of(device));

        mockMvc.perform(get("/core/api/v1/devices")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getDeviceStats_returnsStats() throws Exception {
        given(deviceTokenService.getDeviceStats())
                .willReturn(Map.of("total", 10L, "active", 5L));

        mockMvc.perform(get("/core/api/v1/devices/stats")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void registerDevice_sinRoleApp_retornaForbidden() throws Exception {
        var request = """
                {
                    "token": "fcm-token-123",
                    "platform": "ANDROID"
                }
                """;

        mockMvc.perform(post("/core/api/v1/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isForbidden());
    }
}
