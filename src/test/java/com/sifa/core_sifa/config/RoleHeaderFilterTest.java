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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoleHeaderFilterTest {

    private final RoleHeaderFilter filter = new RoleHeaderFilter();

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

    @Test
    void doFilter_cuandoHeadersPresentes_seteaAuthentication() throws Exception {
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-User")).willReturn("admin@test.cl");
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-Roles")).willReturn("USER_ADMIN,USER_SUPERVISOR");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin@test.cl");
        List<String> authorityStrings = auth.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        assertThat(authorityStrings)
                .containsExactlyInAnyOrder("USER_ADMIN", "USER_SUPERVISOR");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_cuandoSinHeaders_noSeteaAuthentication() throws Exception {
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-User")).willReturn(null);
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-Roles")).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_cuandoRolVacio_noSeteaAuthentication() throws Exception {
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-User")).willReturn("user@test.cl");
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-Roles")).willReturn("");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_cuandoUnSoloRol_seteaUnaAutoridad() throws Exception {
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-User")).willReturn("app@test.cl");
        org.mockito.BDDMockito.given(request.getHeader("X-Auth-Roles")).willReturn("USER_APP");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER_APP");
    }
}
