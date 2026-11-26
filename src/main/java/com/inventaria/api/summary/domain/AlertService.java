package com.inventaria.api.summary.domain;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    public List<AlertDto> generateAlerts(
            List<DaySummaryData.LowStockItem> lowStock,
            List<DaySummaryData.ExpiryItem> expiring) {

        List<AlertDto> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (DaySummaryData.LowStockItem item : lowStock) {
            if (item.stock() == 0) {
                alerts.add(new AlertDto("CRITICO", "STOCK_CERO", item.productId(),
                        "Sin stock: " + item.name(),
                        "Stock: 0 (mínimo: " + item.minimumStock() + ")"));
            } else {
                alerts.add(new AlertDto("AVISO", "STOCK_BAJO", item.productId(),
                        "Stock bajo: " + item.name(),
                        "Stock: " + item.stock() + " (mínimo: " + item.minimumStock() + ")"));
            }
        }

        for (DaySummaryData.ExpiryItem item : expiring) {
            long days = ChronoUnit.DAYS.between(today, item.expirationDate());
            if (days < 0) {
                alerts.add(new AlertDto("CRITICO", "CADUCADO", item.productId(),
                        "Vencido: " + item.name(),
                        "Venció hace " + Math.abs(days) + " día(s): " + item.expirationDate()));
            } else if (days <= 2) {
                alerts.add(new AlertDto("CRITICO", "POR_CADUCAR", item.productId(),
                        "Caduca pronto: " + item.name(),
                        "Vence en " + days + " día(s): " + item.expirationDate()));
            } else {
                alerts.add(new AlertDto("AVISO", "POR_CADUCAR", item.productId(),
                        "Por caducar: " + item.name(),
                        "Vence en " + days + " días: " + item.expirationDate()));
            }
        }

        return alerts;
    }
}
