package com.sifa.core_sifa.controller;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.TestSecurityConfig;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.model.NotificationLog;
import com.sifa.core_sifa.service.notification.INotificationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationLogController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class NotificationLogControllerTest extends ControllerTestBase {

    @MockitoBean
    private INotificationLogService notificationLogService;

    @Test
    void getHistory_returnsPagedResults() throws Exception {
        var log = NotificationLog.builder().targetType("ALL").title("Test").build();
        var page = new PageImpl<>(List.of(log));
        given(notificationLogService.getHistory(any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/notifications/history")
                        .headers(authHeaders("USER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetType").value("ALL"));
    }

    @Test
    void getHistory_withFilters_returnsFiltered() throws Exception {
        var log = NotificationLog.builder().targetType("ALL").title("Test").build();
        var page = new PageImpl<>(List.of(log));
        given(notificationLogService.getHistory(any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/core/api/v1/notifications/history")
                        .param("targetType", "ALL")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .headers(authHeaders("USER_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    void getHistory_sinRole_retornaForbidden() throws Exception {
        mockMvc.perform(get("/core/api/v1/notifications/history")
                        .headers(authHeaders("USER_APP")))
                .andExpect(status().isForbidden());
    }
}
