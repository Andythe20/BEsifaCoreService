package com.sifa.core_sifa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sifa.core_sifa.dto.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

/**
 * Configuración de seguridad del microservicio core-sifa.
 * <p>
 * A diferencia del diseño anterior (que permitía todas las peticiones y delegaba
 * la seguridad a cabeceras planas), aquí {@code anyRequest().authenticated()}
 * exige que toda petición posea una autenticación válida, la cual solo puede
 * construirse si el <b>token interno</b> enviado por el Gateway en el header
 * {@code X-Auth-Identity} es criptográficamente válido.
 * <p>
 * Las únicas rutas públicas son las de documentación Swagger, que el Gateway
 * expone <b>sin</b> inyectar el token interno.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final InternalTokenFilter internalTokenFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(InternalTokenFilter internalTokenFilter, ObjectMapper objectMapper) {
        this.internalTokenFilter = internalTokenFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. API stateless: no usamos cookies de sesión ni CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. Documentación Swagger es pública (el Gateway la sirve sin token interno)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )

                // 3. Respuestas consistentes (JSON) para peticiones no autenticadas / sin permisos
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeError(
                                response, HttpStatus.UNAUTHORIZED, "Token interno inválido o ausente"))
                        .accessDeniedHandler((request, response, ex) -> writeError(
                                response, HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso"))
                )

                // 4. El filtro valida el token interno firmado por el Gateway
                .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe una respuesta de error JSON en el mismo formato {@link ErrorResponse}
     * que usa el resto de la aplicación.
     */
    private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        objectMapper.writeValue(response.getWriter(), error);
    }
}