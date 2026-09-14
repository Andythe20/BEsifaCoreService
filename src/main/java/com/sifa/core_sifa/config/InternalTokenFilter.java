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
import java.util.List;

/**
 * Filtro de autenticación basado en el <b>token interno</b> firmado por el Gateway.
 * <p>
 * Sustituye al antiguo {@code RoleHeaderFilter}, que autenticaba a partir de las
 * cabeceras planas {@code X-Auth-User} / {@code X-Auth-Roles}. Esas cabeceras pueden
 * ser falsificadas por cualquier cliente con acceso directo a la red interna, por lo
 * que ya <b>no se deben confiar en ellas</b>.
 * <p>
 * Este filtro solo crea la autenticación si el token interno ({@code X-Auth-Identity})
 * supera la validación criptográfica (firma + issuer + audience + expiración). Si no
 * hay token o es inválido, la petición circula sin autenticación y {@code SecurityConfig}
 * rechaza el acceso a rutas protegidas con 401.
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String AUTH_IDENTITY_HEADER = "X-Auth-Identity";

    private final InternalJwtUtil internalJwtUtil;

    public InternalTokenFilter(InternalJwtUtil internalJwtUtil) {
        this.internalJwtUtil = internalJwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(AUTH_IDENTITY_HEADER);

        // Solo se autentica si el token interno es válido (emitido y firmado por el Gateway).
        if (token != null && !token.isBlank() && internalJwtUtil.isValid(token)) {

            String email = internalJwtUtil.extractUsername(token);
            List<String> roles = internalJwtUtil.extractRoles(token);

            // Convertimos la lista de roles (ej: ["USER_ADMIN","USER_SUPERVISOR"]) en autoridades de Spring.
            List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}