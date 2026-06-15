package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.device.IDeviceTokenService;
import com.sifa.core_sifa.service.notification.INotificationLogService;
import com.sifa.core_sifa.service.push.IPushService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationPushController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class NotificationPushControllerTest extends ControllerTestBase {

    @MockitoBean
    private IPushService pushService;

    @MockitoBean
    private IDeviceTokenService deviceTokenService;

    @MockitoBean
    private INotificationLogService notificationLogService;

    @Test
    void sendPush_cuandoExito_returnsMessageId() throws Exception {
        given(pushService.send(anyString(), anyString(), anyString())).willReturn("msg-123");

        var request = """
                {
                    "token": "fcm-token",
                    "title": "Test",
                    "body": "Body"
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("msg-123"));
    }

    @Test
    void sendPush_cuandoPushFalla_returnsBadRequest() throws Exception {
        willThrow(new RuntimeException("Push service error"))
                .given(pushService).send(anyString(), anyString(), anyString());

        var request = """
                {
                    "token": "bad-token",
                    "title": "Test",
                    "body": "Body"
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notifyAll_returnsSentCount() throws Exception {
        given(deviceTokenService.notifyAllDevices(anyString(), anyString())).willReturn(5);

        var request = """
                {
                    "title": "Test All",
                    "body": "Body"
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(5));
    }

    @Test
    void notifyByPlatform_returnsSentCount() throws Exception {
        given(deviceTokenService.notifyByPlatform(anyString(), anyString(), anyString())).willReturn(3);

        var request = """
                {
                    "title": "Test Platform",
                    "body": "Body",
                    "targetPlatform": "ANDROID"
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push/platform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("ANDROID"));
    }

    @Test
    void listDevices_returnsList() throws Exception {
        var device = DeviceTokenResponse.builder()
                .id(1L).emailUsuario("user@test.cl").platform("ANDROID").build();
        given(deviceTokenService.getAllDeviceResponses()).willReturn(List.of(device));

        mockMvc.perform(get("/core/api/v1/notifications/push/devices")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void notifySelect_returnsSentCount() throws Exception {
        given(deviceTokenService.notifyDevicesByIds(any(), anyString(), anyString())).willReturn(2);

        var request = """
                {
                    "title": "Test Select",
                    "body": "Body",
                    "deviceIds": [1, 2]
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(2));
    }

    @Test
    void notifyOutdated_returnsSentCount() throws Exception {
        given(deviceTokenService.notifyOutdatedDevices(anyString(), anyString(), anyString())).willReturn(3);

        var request = """
                {
                    "currentVersion": "2.0.0",
                    "title": "Update Available",
                    "body": "Please update"
                }
                """;

        mockMvc.perform(post("/core/api/v1/notifications/push/outdated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .header("X-Auth-User", "admin@test.cl")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value("2.0.0"));
    }

    @Test
    void sendPush_sinRoleValido_retornaForbidden() throws Exception {
        mockMvc.perform(post("/core/api/v1/notifications/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"title\":\"x\",\"body\":\"x\"}")
                        .headers(authHeaders("USER_JPL")))
                .andExpect(status().isForbidden());
    }
}
