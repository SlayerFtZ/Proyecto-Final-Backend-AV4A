package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                resource + " con id " + id + " no fue encontrado");
    }
}
