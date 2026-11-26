package com.inventaria.api.products.presentation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchRequest(

        @NotNull(message = "La lista de productos es obligatoria")
        @NotEmpty(message = "La lista no puede estar vacía")
        @Size(max = 500, message = "El lote no puede exceder 500 productos")
        List<ProductRequest> products
) {}
