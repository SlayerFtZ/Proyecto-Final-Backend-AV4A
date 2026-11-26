package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class AiUnavailableException extends DomainException {

    public AiUnavailableException(String reason) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "IA_NO_DISPONIBLE",
                "El servicio de inteligencia artificial no está disponible: " + reason);
    }
}
