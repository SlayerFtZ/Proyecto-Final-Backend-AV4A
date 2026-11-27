package com.inventaria.api.summary.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceTest {

    private final AlertService service = new AlertService();

    // ── Stock alerts ─────────────────────────────────────────────────────────

    @Test
    void stockZero_generatesCriticoAlert() {
        var item = new DaySummaryData.LowStockItem(1, "Leche", 0, 5, "Perecedero");
        List<AlertDto> alerts = service.generateAlerts(List.of(item), List.of());

        assertEquals(1, alerts.size());
        assertEquals("CRITICO", alerts.get(0).level());
        assertEquals("STOCK_CERO", alerts.get(0).type());
    }

    @Test
    void stockBelowMinimum_generatesAvisoAlert() {
        var item = new DaySummaryData.LowStockItem(1, "Pan", 2, 5, "Perecedero");
        List<AlertDto> alerts = service.generateAlerts(List.of(item), List.of());

        assertEquals(1, alerts.size());
        assertEquals("AVISO", alerts.get(0).level());
        assertEquals("STOCK_BAJO", alerts.get(0).type());
    }

    // ── Expiry alerts ─────────────────────────────────────────────────────────

    @Test
    void expiredProduct_generatesCriticoAlert() {
        var item = new DaySummaryData.ExpiryItem(1, "Yogurt", LocalDate.now().minusDays(2), 3);
        List<AlertDto> alerts = service.generateAlerts(List.of(), List.of(item));

        assertEquals(1, alerts.size());
        assertEquals("CRITICO", alerts.get(0).level());
        assertEquals("CADUCADO", alerts.get(0).type());
    }

    @Test
    void expiresInTwoDays_generatesCriticoAlert() {
        var item = new DaySummaryData.ExpiryItem(1, "Queso", LocalDate.now().plusDays(2), 5);
        List<AlertDto> alerts = service.generateAlerts(List.of(), List.of(item));

        assertEquals("CRITICO", alerts.get(0).level());
        assertEquals("POR_CADUCAR", alerts.get(0).type());
    }

    @Test
    void expiresInThreeDays_generatesAvisoAlert() {
        var item = new DaySummaryData.ExpiryItem(1, "Mantequilla", LocalDate.now().plusDays(3), 4);
        List<AlertDto> alerts = service.generateAlerts(List.of(), List.of(item));

        assertEquals("AVISO", alerts.get(0).level());
        assertEquals("POR_CADUCAR", alerts.get(0).type());
    }

    @Test
    void expiresInSevenDays_generatesAvisoAlert() {
        var item = new DaySummaryData.ExpiryItem(1, "Crema", LocalDate.now().plusDays(7), 2);
        List<AlertDto> alerts = service.generateAlerts(List.of(), List.of(item));

        assertEquals("AVISO", alerts.get(0).level());
        assertEquals("POR_CADUCAR", alerts.get(0).type());
    }

    @Test
    void emptyData_returnsEmptyAlerts() {
        List<AlertDto> alerts = service.generateAlerts(List.of(), List.of());
        assertTrue(alerts.isEmpty());
    }
}
