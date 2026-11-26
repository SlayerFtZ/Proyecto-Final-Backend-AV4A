package com.inventaria.api.products.domain;

import com.inventaria.api.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class TypeValidationException extends DomainException {

    public TypeValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDACION_TIPO", message);
    }
}
