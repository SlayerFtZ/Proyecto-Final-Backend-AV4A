package com.inventaria.api.summary.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalSummaryGeneratorTest {

    private final LocalSummaryGenerator generator = new LocalSummaryGenerator();

    @Test
    void generate_normalData_returnsNonEmptyText() {
        DaySummaryData data = new DaySummaryData(5, new BigDecimal("250.00"),
                List.of(new DaySummaryData.LowStockItem(1, "Leche", 2, 5, "Perecedero")),
                List.of(new DaySummaryData.ExpiryItem(2, "Yogurt", LocalDate.now().plusDays(1), 3)),
                List.of(new DaySummaryData.TopSellerItem(3, "Agua", 20L)));

        List<AlertDto> alerts = List.of(
                new AlertDto("CRITICO", "STOCK_CERO", 1, "Sin stock", "detail"),
                new AlertDto("AVISO", "STOCK_BAJO", 2, "Stock bajo", "detail"));

        String text = generator.generate(data, alerts);

        assertNotNull(text);
        assertFalse(text.isBlank());
        assertTrue(text.contains("5"));      // sales count
        assertTrue(text.contains("250"));    // revenue
    }

    @Test
    void generate_emptyData_noAlerts_returnsPositiveRecommendation() {
        DaySummaryData data = new DaySummaryData(0, BigDecimal.ZERO,
                List.of(), List.of(), List.of());

        String text = generator.generate(data, List.of());

        assertNotNull(text);
        assertTrue(text.contains("buen estado"));
    }

    @Test
    void generate_withCriticalAlerts_includesSpecificRecommendations() {
        DaySummaryData data = new DaySummaryData(0, BigDecimal.ZERO,
                List.of(new DaySummaryData.LowStockItem(1, "X", 0, 5, "Perecedero")),
                List.of(new DaySummaryData.ExpiryItem(2, "Y", LocalDate.now().minusDays(1), 1)),
                List.of());

        List<AlertDto> alerts = List.of(
                new AlertDto("CRITICO", "STOCK_CERO", 1, "Sin stock", "d"),
                new AlertDto("CRITICO", "CADUCADO", 2, "Vencido", "d"));

        String text = generator.generate(data, alerts);

        assertTrue(text.contains("Reabastecer"));
        assertTrue(text.contains("vencidos"));
    }
}
