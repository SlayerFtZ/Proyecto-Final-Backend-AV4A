package com.inventaria.api.sales.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaleRequest(

        @NotNull(message = "Los artículos de la venta son obligatorios")
        @NotEmpty(message = "La venta debe tener al menos un artículo")
        List<@Valid SaleItemRequest> items
) {}
