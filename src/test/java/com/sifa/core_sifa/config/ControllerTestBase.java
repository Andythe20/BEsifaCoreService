package com.sifa.core_sifa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

/**
 * Base común para los tests de controladores.
 * <p>
 * Provee cabeceras de autenticación válidas para core-sifa: genera un token interno
 * ({@code X-Auth-Identity}) firmado con el secreto de TEST, replicando el
 * comportamiento del API Gateway. Las cabeceras planas {@code X-Auth-User} y
 * {@code X-Auth-Roles} se incluyen solo por retrocompatibilidad.
 */
public abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected HttpHeaders authHeaders(String... roles) {
        return authHeadersFor("admin@sifa.cl", roles);
    }

    protected HttpHeaders authHeadersFor(String email, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Auth-Identity", InternalTokenTestSupport.generateInternalToken(email, List.of(roles)));
        headers.add("X-Auth-User", email);
        headers.add("X-Auth-Roles", String.join(",", roles));
        return headers;
    }
}