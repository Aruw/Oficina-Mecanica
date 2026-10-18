package com.aruw.oficina.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("app.security.jwt.secret (JWT_SECRET) deve ter pelo menos 32 caracteres");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "oficina-api";
        }
        if (expiration == null) {
            expiration = Duration.ofHours(2);
        }
    }
}
