package com.inventaria.api.scanner.domain;

public record ProductSuggestionResponse(
        String suggestedName,
        String suggestedCategory,
        Integer suggestedCategoryId,     // null if no DB match found
        String productType,              // "Perecedero" | "Electronico" | "Ropa" | null
        String description
) {}
