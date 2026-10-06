package com.sifa.core_sifa.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Valida la guardia del canal interno: sin la cabecera {@code X-Internal-Key}
 * firmada con {@code INTERNAL_CHANNEL_KEY} la petición no pasa, aunque venga de
 * la IP privada y porte un {@code X-Auth-Identity} convincente.
 */
@ExtendWith(MockitoExtension.class)
class ChannelKeyFilterTest {

    private static final String KEY = "clave-compartida-de-canal-0123456789";

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest requestFrom(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.setRequestURI("/api/v1/alguna-ruta");
        return request;
    }

    @Test
    void doFilter_conCabeceraValida_continua() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter(KEY);
        MockHttpServletRequest request = requestFrom("10.0.2.10");
        request.addHeader("X-Internal-Key", KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_sinCabecera_responde403() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter(KEY);
        MockHttpServletRequest request = requestFrom("10.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Canal interno");
    }

    @Test
    void doFilter_conCabeceraIncorrecta_responde403() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter(KEY);
        MockHttpServletRequest request = requestFrom("10.0.2.10");
        request.addHeader("X-Internal-Key", "otra-clave-completamente-distinta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    /** Interruptor de apagado: sin clave configurada la guardia no bloquea. */
    @Test
    void doFilter_sinClaveConfigurada_continua() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter("");
        MockHttpServletRequest request = requestFrom("10.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_desdeLoopbackIPv4_continua() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter(KEY);
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_desdeLoopbackIPv6_continua() throws Exception {
        ChannelKeyFilter filter = new ChannelKeyFilter(KEY);
        MockHttpServletRequest request = requestFrom("0:0:0:0:0:0:0:1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
