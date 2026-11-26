package com.inventaria.api.summary.domain;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LocalSummaryGenerator {

    public String generate(DaySummaryData data, List<AlertDto> alerts) {
        try {
            return buildText(data, alerts);
        } catch (Exception e) {
            return "Resumen del día no disponible (error al generar el texto local).";
        }
    }

    private String buildText(DaySummaryData data, List<AlertDto> alerts) {
        StringBuilder sb = new StringBuilder("Resumen del día — InventarIA\n\n");

        sb.append("Ventas hoy: ").append(data.totalSalesToday())
          .append(" transacción(es), total $")
          .append(String.format("%.2f", data.totalRevenue())).append(".\n");

        if (!data.topSellers().isEmpty()) {
            String sellers = data.topSellers().stream()
                    .map(ts -> ts.name() + " ×" + ts.unitsSold())
                    .collect(Collectors.joining(", "));
            sb.append("Más vendidos: ").append(sellers).append(".\n");
        }

        long criticos = alerts.stream().filter(a -> "CRITICO".equals(a.level())).count();
        long avisos   = alerts.stream().filter(a -> "AVISO".equals(a.level())).count();
        if (criticos > 0 || avisos > 0) {
            sb.append("\nAlertas: ").append(criticos).append(" crítica(s), ")
              .append(avisos).append(" aviso(s).\n");
        }

        sb.append("\nRecomendaciones:\n");
        boolean hasStockZero = alerts.stream().anyMatch(a -> "STOCK_CERO".equals(a.type()));
        boolean hasExpired   = alerts.stream().anyMatch(a -> "CADUCADO".equals(a.type()));
        boolean hasLowStock  = alerts.stream().anyMatch(a -> "STOCK_BAJO".equals(a.type()));
        boolean hasSoonExp   = alerts.stream().anyMatch(a -> "POR_CADUCAR".equals(a.type()));

        if (hasStockZero) sb.append("- Reabastecer de inmediato los productos sin stock.\n");
        if (hasExpired)   sb.append("- Retirar y desechar los productos vencidos.\n");
        if (hasLowStock)  sb.append("- Generar órdenes de compra para los productos con stock bajo.\n");
        if (hasSoonExp)   sb.append("- Priorizar la venta de productos próximos a caducar.\n");
        if (criticos == 0 && avisos == 0)
            sb.append("- Inventario en buen estado. Continuar con el monitoreo regular.\n");

        return sb.toString();
    }
}
