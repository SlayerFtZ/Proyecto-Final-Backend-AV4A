package com.inventaria.api.auth.infrastructure;

import com.inventaria.api.auth.domain.GoogleTokenVerifier;
import com.inventaria.api.auth.domain.GoogleUserInfo;
import com.inventaria.api.auth.domain.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Verifica id_tokens de Google localmente con las llaves públicas de Google (JWKS).
 * Las llaves se descargan y se cachean; no se llama a Google por cada login.
 */
@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder decoder;

    public GoogleTokenVerifierImpl(@Value("${app.security.google-client-ids}") String clientIdsProperty) {
        List<String> clientIds = Arrays.stream(clientIdsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (clientIds.isEmpty()) {
            throw new IllegalStateException("app.security.google-client-ids (GOOGLE_CLIENT_IDS) está vacío");
        }

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.stream().anyMatch(clientIds::contains))
        );

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        nimbus.setJwtValidator(validator);
        this.decoder = nimbus;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new InvalidGoogleTokenException("El id_token de Google es inválido o expiró", e);
        }

        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new InvalidGoogleTokenException("El email de la cuenta de Google no está verificado");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new InvalidGoogleTokenException("El id_token no contiene email");
        }

        return new GoogleUserInfo(
                jwt.getSubject(),
                email,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture")
        );
    }
}
