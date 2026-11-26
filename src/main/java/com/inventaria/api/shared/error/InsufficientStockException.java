package com.inventaria.api.shared.error;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(String productName, int available, int requested) {
        super(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                "Stock insuficiente para '" + productName + "': disponible " + available + ", solicitado " + requested);
    }

    public InsufficientStockException(int productId) {
        super(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                "Stock insuficiente para el producto con id " + productId);
    }
}
