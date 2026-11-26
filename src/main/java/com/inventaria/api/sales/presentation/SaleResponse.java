package com.inventaria.api.sales.presentation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Integer id,
        LocalDateTime fecha,
        BigDecimal total,
        List<SaleItemResponse> items
) {}
