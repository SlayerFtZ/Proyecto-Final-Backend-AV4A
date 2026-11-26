package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends DomainException {

    public InvalidFileException(String message) {
        super(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO", message);
    }

    public InvalidFileException(HttpStatus status, String message) {
        super(status, "ARCHIVO_INVALIDO", message);
    }
}
