package com.inventaria.api.sales.presentation;

import java.math.BigDecimal;

public record SaleItemResponse(
        Integer id,
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
