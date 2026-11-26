package com.inventaria.api.products.presentation;

import com.inventaria.api.products.domain.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Integer id,
        String barcode,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Integer minimumStock,
        ProductType type,
        LocalDate expirationDate,
        Integer warrantyMonths,
        String size,
        Integer categoryId,
        String categoryName,
        Integer supplierId,
        String supplierName
) {}
