package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class DuplicateNameException extends DomainException {

    public DuplicateNameException(String resourceType, String name) {
        super(HttpStatus.CONFLICT, "NOMBRE_DUPLICADO",
                "Ya existe un(a) " + resourceType + " con el nombre '" + name + "'");
    }
}
