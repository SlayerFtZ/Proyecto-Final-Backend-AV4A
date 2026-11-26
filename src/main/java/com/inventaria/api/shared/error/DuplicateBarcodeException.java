package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class DuplicateBarcodeException extends DomainException {

    public DuplicateBarcodeException(String barcode) {
        super(HttpStatus.CONFLICT, "CODIGO_BARRAS_DUPLICADO",
                "El código de barras '" + barcode + "' ya está registrado");
    }
}
