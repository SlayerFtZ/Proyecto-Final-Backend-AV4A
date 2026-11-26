package com.inventaria.api.products.domain;

public record ProductFilter(
        String query,
        Integer categoryId,
        Integer supplierId,
        ProductType type
) {
    public static ProductFilter empty() {
        return new ProductFilter(null, null, null, null);
    }
}
