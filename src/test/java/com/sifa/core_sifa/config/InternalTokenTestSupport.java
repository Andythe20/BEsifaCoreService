package com.sifa.core_sifa.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Helper de tests para generar tokens internos equivalentes a los que emite el
 * API Gateway, usando el secreto interno definido en {@code application-test.properties}.
 */
public final class InternalTokenTestSupport {

    public static final String SECRET = "U2VjcmV0RGVFbnRvcm5vU2lmYTIwMjZTdXBlclNlZ3Vyb1BhcmFDb3JlU2lmYQ==";
    public static final String ISSUER = "sifa-gateway";
    public static final String AUDIENCE = "sifa-core";

    private InternalTokenTestSupport() {
    }

    /**
     * Genera un token interno válido (secreto, issuer y audience de test).
     */
    public static String generateInternalToken(String subject, List<String> roles) {
        return generateInternalToken(SECRET, subject, roles, ISSUER, AUDIENCE, Instant.now().plusSeconds(60));
    }

    /**
     * Genera un token interno con parámetros personalizados para casos de prueba
     * (secreto distinto, issuer incorrecto, token expirado, etc.).
     */
    public static String generateInternalToken(String secret, String subject, List<String> roles,
                                               String issuer, String audience, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(Instant.now().minusSeconds(5)))
                .expiration(Date.from(expiry))
                .claim("roles", roles)
                .signWith(signingKey(secret))
                .compact();
    }

    private static SecretKey signingKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}