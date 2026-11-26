package com.inventaria.api.summary.domain;

import java.time.LocalDateTime;
import java.util.List;

public record SummaryResponse(
        LocalDateTime generatedAt,
        String origin,      // "IA" | "LOCAL" | null — omitted in JSON for /datos
        String text,        // null for /datos — omitted by non_null Jackson config
        List<AlertDto> alerts,
        DaySummaryData data
) {}
