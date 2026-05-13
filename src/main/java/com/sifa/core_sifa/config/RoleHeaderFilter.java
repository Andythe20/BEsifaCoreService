package com.sifa.core_sifa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class RoleHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String email = request.getHeader("X-Auth-User");
        String rolesHeader = request.getHeader("X-Auth-Roles");

        // Si el Gateway envió el usuario y sus roles, los registramos en Spring Security
        if (email != null && rolesHeader != null && !rolesHeader.isEmpty()) {

            // Convertimos el texto "ROLE_ADMIN,ROLE_FISCALIZADOR" en autoridades de Spring
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            // Creamos el token de autenticación (sin contraseña porque ya se validó)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            // Poblamos el contexto de seguridad (esto permite usar @PreAuthorize)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}