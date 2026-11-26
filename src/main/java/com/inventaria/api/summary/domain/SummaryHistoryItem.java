package com.inventaria.api.summary.domain;

import java.time.LocalDateTime;

public record SummaryHistoryItem(Integer id, LocalDateTime fecha, String text) {}
