package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.dto.audit.AuditLogResponseDTO;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.audits.IAuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuditLogControllerTest extends ControllerTestBase {

    @MockitoBean
    private IAuditLogService auditLogService;

    @Test
    void crearLogInterno_returnsOk() throws Exception {
        var request = """
                {
                    "emailUsuario": "admin@test.cl",
                    "accion": "PROCESAR_INFRACCION",
                    "tablaAfectada": "INFRACCIONES",
                    "idRegistroAfectado": "1",
                    "detalles": {"estado": "APROBADA"}
                }
                """;

        mockMvc.perform(post("/core/api/v1/internal/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    @Test
    void crearLogInterno_sinAccion_returnsBadRequest() throws Exception {
        var request = """
                {
                    "emailUsuario": "admin@test.cl",
                    "detalles": {"estado": "APROBADA"}
                }
                """;

        mockMvc.perform(post("/core/api/v1/internal/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearLogInterno_sinDetalles_returnsBadRequest() throws Exception {
        var request = """
                {
                    "emailUsuario": "admin@test.cl",
                    "accion": "PROCESAR_INFRACCION"
                }
                """;

        mockMvc.perform(post("/core/api/v1/internal/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_returnsPagedResults() throws Exception {
        var log = AuditLogResponseDTO.builder()
                .email_usuario("admin@test.cl")
                .accion("PROCESAR_INFRACCION")
                .build();
        var page = new PageImpl<>(List.of(log));
        given(auditLogService.findAll(any(), any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/internal/audit")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accion").value("PROCESAR_INFRACCION"));
    }

    @Test
    void findAll_withInvalidDateRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/core/api/v1/internal/audit")
                        .param("startDate", "2024-12-31")
                        .param("endDate", "2024-01-01")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_sinRoleAdmin_retornaForbidden() throws Exception {
        mockMvc.perform(get("/core/api/v1/internal/audit")
                        .headers(authHeaders("USER_APP")))
                .andExpect(status().isForbidden());
    }
}
