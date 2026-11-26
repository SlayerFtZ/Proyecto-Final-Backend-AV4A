package com.inventaria.api.sales.presentation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(

        @NotNull(message = "El ID del producto es obligatorio")
        Integer productId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer quantity,

        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0 si se especifica")
        BigDecimal unitPrice
) {}
