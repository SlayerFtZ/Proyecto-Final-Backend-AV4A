package com.inventaria.api.products.presentation;

import com.inventaria.api.products.domain.ProductType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(

        @NotBlank(message = "El código de barras es obligatorio")
        @Pattern(regexp = "^\\d{8,14}$", message = "El código de barras debe tener entre 8 y 14 dígitos")
        String barcode,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal price,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer minimumStock,

        @NotNull(message = "El tipo es obligatorio")
        ProductType type,

        LocalDate expirationDate,

        @Min(value = 0, message = "Los meses de garantía no pueden ser negativos")
        Integer warrantyMonths,

        @Size(max = 10, message = "La talla no puede exceder 10 caracteres")
        String size,

        @NotNull(message = "La categoría es obligatoria")
        Integer categoryId,

        @NotNull(message = "El proveedor es obligatorio")
        Integer supplierId
) {}
