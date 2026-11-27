package com.inventaria.api.auth.domain;

public interface GoogleTokenVerifier {

    /**
     * Verifica firma, emisor, audiencia y expiración del id_token de Google.
     *
     * @throws InvalidGoogleTokenException si el token no es válido
     */
    GoogleUserInfo verify(String idToken);
}
