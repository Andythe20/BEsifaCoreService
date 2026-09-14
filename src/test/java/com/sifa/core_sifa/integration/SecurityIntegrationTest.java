package com.sifa.core_sifa.integration;

import com.sifa.core_sifa.config.ControllerTestBase;
import com.sifa.core_sifa.config.InternalJwtUtil;
import com.sifa.core_sifa.config.InternalTokenFilter;
import com.sifa.core_sifa.config.InternalTokenTestSupport;
import com.sifa.core_sifa.config.SecurityConfig;
import com.sifa.core_sifa.controller.VehiculoController;
import com.sifa.core_sifa.dto.VehiculoDTO;
import com.sifa.core_sifa.exception.GlobalExceptionHandler;
import com.sifa.core_sifa.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de seguridad de acceso directo: core-sifa NO debe confiar en cabeceras
 * planas. Un atacante con acceso directo a la red interna que inyecte
 * {@code X-Auth-User} / {@code X-Auth-Roles} falsas debe ser rechazado (401),
 * aunque el endpoint esté protegido por {@code @PreAuthorize}.
 * <p>
 * Solo las peticiones que lleven un token interno ({@code X-Auth-Identity}) válido
 * y con el rol requerido son aceptadas.
 */
@WebMvcTest(VehiculoController.class)
@Import({SecurityConfig.class, InternalJwtUtil.class, InternalTokenFilter.class,
        GlobalExceptionHandler.class})
class SecurityIntegrationTest extends ControllerTestBase {

    @MockitoBean
    private VehiculoService vehiculoService;

    @Test
    void accesoDirecto_conCabecerasFalsificadas_retorna401() throws Exception {
        given(vehiculoService.findAllVehiculos()).willReturn(List.of(buildVehiculo()));

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .header("X-Auth-User", "admin@hacker.com")
                        .header("X-Auth-Roles", "USER_ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoDirecto_sinCabeceras_retorna401() throws Exception {
        mockMvc.perform(get("/core/api/v1/vehiculos/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoDirecto_conTokenFirmadoConOtraLlave_retorna401() throws Exception {
        given(vehiculoService.findAllVehiculos()).willReturn(List.of(buildVehiculo()));

        String tokenForjado = InternalTokenTestSupport.generateInternalToken(
                "Otr0SecretoDiferenteParaFalsificacionDeFirma123",
                "admin@hacker.com", List.of("USER_ADMIN"),
                InternalTokenTestSupport.ISSUER, InternalTokenTestSupport.AUDIENCE,
                Instant.now().plusSeconds(60));

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .header("X-Auth-Identity", tokenForjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoDirecto_conTokenValidoYRolPermitido_retorna200() throws Exception {
        given(vehiculoService.findAllVehiculos()).willReturn(List.of(buildVehiculo()));

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .header("X-Auth-Identity",
                                InternalTokenTestSupport.generateInternalToken(
                                        "fiscalizador@test.cl", List.of("USER_APP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void accesoDirecto_conTokenValidoPeroRolNoPermitido_retorna403() throws Exception {
        given(vehiculoService.findAllVehiculos()).willReturn(List.of(buildVehiculo()));

        mockMvc.perform(get("/core/api/v1/vehiculos/all")
                        .header("X-Auth-Identity",
                                InternalTokenTestSupport.generateInternalToken(
                                        "supervisor@test.cl", List.of("USER_SUPERVISOR"))))
                .andExpect(status().isForbidden());
    }

    /**
     * OpenAPI no se expone en el contexto de {@link WebMvcTest}, por lo que la
     * expectativa es 404. Lo relevante es que la ruta NO devuelva 401/403: la
     * configuracion de seguridad la deja fuera de {@code anyRequest().authenticated()}.
     */
    @Test
    void rutaSwagger_esPublicaSinToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    private VehiculoDTO buildVehiculo() {
        return VehiculoDTO.builder()
                .patente("AAAA11")
                .marca("TEST")
                .modelo("UNIT")
                .build();
    }
}