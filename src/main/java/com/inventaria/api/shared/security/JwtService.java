package com.inventaria.api.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite el JWT propio de la API (HS256). */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration expiration;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.security.jwt-issuer}") String issuer,
                      @Value("${app.security.jwt-expiration-hours}") long expirationHours) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expiration = Duration.ofHours(expirationHours);
    }

    public IssuedToken issue(Integer userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))   // IdUsuario de la tabla Usuarios
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", email)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
