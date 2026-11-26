package com.inventaria.api.summary.domain;

public record AlertDto(
        String level,
        String type,
        Integer productId,
        String title,
        String detail
) {}
