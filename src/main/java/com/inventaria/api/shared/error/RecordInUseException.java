package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class RecordInUseException extends DomainException {

    public RecordInUseException(String message) {
        super(HttpStatus.CONFLICT, "REGISTRO_EN_USO", message);
    }

    public RecordInUseException(String resource, Object id) {
        super(HttpStatus.CONFLICT, "REGISTRO_EN_USO",
                "No se puede eliminar el " + resource + " con id " + id + " porque tiene registros asociados");
    }
}
