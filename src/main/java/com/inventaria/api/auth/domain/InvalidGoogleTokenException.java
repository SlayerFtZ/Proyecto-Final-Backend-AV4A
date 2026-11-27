package com.inventaria.api.auth.domain;

import com.inventaria.api.shared.error.DomainException;
import org.springframework.http.HttpStatus;

/** id_token de Google inválido → 401 con el formato ApiError (GlobalExceptionHandler). */
public class InvalidGoogleTokenException extends DomainException {

    public InvalidGoogleTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_GOOGLE_INVALIDO", message);
    }

    public InvalidGoogleTokenException(String message, Throwable cause) {
        this(message);
        initCause(cause);
    }
}
