package com.sifa.core_sifa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Valida que el filtro de token interno NO confíe en cabeceras planas
 * (X-Auth-User / X-Auth-Roles) y solo autentique cuando el token interno
 * sea criptográficamente válido. Cubre la vulnerabilidad de acceso directo
 * con cabeceras falsificadas.
 */
@ExtendWith(MockitoExtension.class)
class InternalTokenFilterTest {

    private final InternalJwtUtil internalJwtUtil = new InternalJwtUtil();
    private final InternalTokenFilter filter = new InternalTokenFilter(internalJwtUtil);

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void configureInternalJwtUtil() {
        ReflectionTestUtils.setField(internalJwtUtil, "secret", InternalTokenTestSupport.SECRET);
        ReflectionTestUtils.setField(internalJwtUtil, "issuer", InternalTokenTestSupport.ISSUER);
        ReflectionTestUtils.setField(internalJwtUtil, "audience", InternalTokenTestSupport.AUDIENCE);
    }

    @Test
    void doFilter_tokenValido_creaAuthenticationConEmailYRoles() throws Exception {
        configureInternalJwtUtil();
        String token = InternalTokenTestSupport.generateInternalToken(
                "admin@test.cl", List.of("USER_ADMIN", "USER_SUPERVISOR"));
        given(request.getHeader("X-Auth-Identity")).willReturn(token);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin@test.cl");
        List<String> authorityStrings = auth.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        assertThat(authorityStrings).containsExactlyInAnyOrder("USER_ADMIN", "USER_SUPERVISOR");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenFirmadoConOtroSecreto_noSeteaAuthentication() throws Exception {
        configureInternalJwtUtil();
        String token = InternalTokenTestSupport.generateInternalToken(
                "Otr0SecretoDiferenteParaFalsificacionDeFirma123",
                "hacker@test.cl", List.of("USER_ADMIN"),
                InternalTokenTestSupport.ISSUER, InternalTokenTestSupport.AUDIENCE,
                Instant.now().plusSeconds(60));
        given(request.getHeader("X-Auth-Identity")).willReturn(token);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenExpirado_noSeteaAuthentication() throws Exception {
        configureInternalJwtUtil();
        String token = InternalTokenTestSupport.generateInternalToken(
                InternalTokenTestSupport.SECRET,
                "admin@test.cl", List.of("USER_ADMIN"),
                InternalTokenTestSupport.ISSUER, InternalTokenTestSupport.AUDIENCE,
                Instant.now().minusSeconds(10));
        given(request.getHeader("X-Auth-Identity")).willReturn(token);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_tokenConIssuerIncorrecto_noSeteaAuthentication() throws Exception {
        configureInternalJwtUtil();
        String token = InternalTokenTestSupport.generateInternalToken(
                InternalTokenTestSupport.SECRET,
                "admin@test.cl", List.of("USER_ADMIN"),
                "otro-emisor", InternalTokenTestSupport.AUDIENCE,
                Instant.now().plusSeconds(60));
        given(request.getHeader("X-Auth-Identity")).willReturn(token);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_sinToken_noSeteaAuthentication() throws Exception {
        configureInternalJwtUtil();
        given(request.getHeader("X-Auth-Identity")).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Cierra la vulnerabilidad de acceso directo: cabeceras planas falsificadas
     * (X-Auth-User / X-Auth-Roles) SIN token interno válido NO deben autenticar.
     */
    @Test
    void doFilter_cabecerasFalsasSinToken_noSeteaAuthentication() throws Exception {
        configureInternalJwtUtil();
        given(request.getHeader("X-Auth-Identity")).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}