package com.sifa.core_sifa.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad para los tests de controladores ({@code @WebMvcTest}).
 * <p>
 * Replica la configuración de producción: exige autenticación ({@code authenticated()})
 * y resuelve la identidad a partir del token interno {@code X-Auth-Identity}. Así los
 * tests validan los mismos {@code @PreAuthorize} que corren en producción.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public InternalJwtUtil internalJwtUtil() {
        return new InternalJwtUtil();
    }

    @Bean
    public InternalTokenFilter internalTokenFilter(InternalJwtUtil internalJwtUtil) {
        return new InternalTokenFilter(internalJwtUtil);
    }

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http, InternalTokenFilter internalTokenFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}