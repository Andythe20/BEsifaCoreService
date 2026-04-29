package com.example.core_sifa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RoleHeaderFilter roleHeaderFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Deshabilitamos CSRF porque nuestra API no usa cookies de sesión
                .csrf(csrf -> csrf.disable())

                // 2. Indicamos que la aplicación no debe guardar estados de sesión
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Permitimos TODAS las peticiones.
                // (La seguridad ya la garantizó el API Gateway antes de que el tráfico llegue aquí)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 4. Todas las peticiones pasan por el filtro personalizado
                .addFilterBefore(roleHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
