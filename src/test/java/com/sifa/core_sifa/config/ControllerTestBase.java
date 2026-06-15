package com.sifa.core_sifa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

public abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected HttpHeaders authHeaders(String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Auth-User", "admin@sifa.cl");
        headers.add("X-Auth-Roles", String.join(",", roles));
        return headers;
    }

    protected HttpHeaders authHeaders() {
        return authHeaders("USER_ADMIN");
    }
}
