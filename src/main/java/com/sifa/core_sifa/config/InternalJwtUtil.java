package com.sifa.core_sifa.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utilidad para validar el <b>token interno</b> emitido por el API Gateway.
 * <p>
 * A diferencia del JWT de sesión del usuario (generado por auth-service), este token
 * es un JWT de <b>corta duración</b> (60s) que el Gateway genera únicamente para
 * transmitir la identidad verificada del usuario a los servicios downstream.
 * <p>
 * El core-sifa NO debe confiar en las cabeceras planas {@code X-Auth-User} /
 * {@code X-Auth-Roles} porque un cliente con acceso directo a la red interna podría
 * falsificarlas. Solo se acepta la identidad si viene firmada con el secreto interno
 * ({@code INTERNAL_JWT_SECRET}) y con issuer/audience esperados.
 */
@Component
public class InternalJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(InternalJwtUtil.class);

    @Value("${jwt.internal.secret}")
    private String secret;

    @Value("${jwt.internal.issuer}")
    private String issuer;

    @Value("${jwt.internal.audience}")
    private String audience;

    /**
     * Deriva la clave HMAC para validar la firma del token interno.
     * Acepta el secreto en Base64 (formato usado en el resto del sistema) o plano.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Valida que el token interno sea auténtico: firma HS256, issuer, audience y
     * expiración. Cualquier fallo (token ausente, manipulado, expirado o emitido
     * por otro emisor) devuelve {@code false}.
     *
     * @param token token interno del header {@code X-Auth-Identity}
     * @return {@code true} si el token es válido y fue emitido por el Gateway
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token interno inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el subject (email del usuario) de un token interno ya validado.
     *
     * @param token token interno
     * @return email del usuario autenticado, o {@code null} si no pudo extraerse
     */
    public String extractUsername(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            log.warn("No se pudo extraer el subject del token interno: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae la lista de roles del token interno ya validado.
     *
     * @param token token interno
     * @return lista de roles (ej: {@code [USER_ADMIN]}), o {@code null} si no pudo extraerse
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractClaims(token);
            return (List<String>) claims.get("roles", List.class);
        } catch (Exception e) {
            log.warn("No se pudieron extraer los roles del token interno: {}", e.getMessage());
            return null;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}