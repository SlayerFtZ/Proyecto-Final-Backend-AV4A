package com.inventaria.api.products.presentation;

import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull(message = "El delta de stock es obligatorio")
        Integer delta
) {}
