package com.inventaria.api.summary.domain;

import com.inventaria.api.shared.config.IaProperties;
import com.inventaria.api.summary.infrastructure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private static final String SYSTEM_PROMPT =
            "Eres un asistente de inventario. Redacta un reporte breve en español " +
            "(máx. 150 palabras) con resumen de ventas, alertas y 2–3 recomendaciones. " +
            "Usa solo los datos recibidos. Responde en texto plano.";

    private final SummaryQueryRepository queryRepository;
    private final AlertService alertService;
    private final AiTextClient aiTextClient;
    private final LocalSummaryGenerator localGenerator;
    private final IaProperties iaProperties;
    private final SummaryIaJpaRepository iaRepository;

    public SummaryService(SummaryQueryRepository queryRepository,
                          AlertService alertService,
                          AiTextClient aiTextClient,
                          LocalSummaryGenerator localGenerator,
                          IaProperties iaProperties,
                          SummaryIaJpaRepository iaRepository) {
        this.queryRepository = queryRepository;
        this.alertService    = alertService;
        this.aiTextClient    = aiTextClient;
        this.localGenerator  = localGenerator;
        this.iaProperties    = iaProperties;
        this.iaRepository    = iaRepository;
    }

    public SummaryResponse getDayData() {
        DaySummaryData data   = fetchDayData();
        List<AlertDto> alerts = alertService.generateAlerts(data.lowStockProducts(), data.expiringProducts());
        return new SummaryResponse(LocalDateTime.now(), null, null, alerts, data);
    }

    @Transactional
    public SummaryResponse generateSummary() {
        DaySummaryData data   = fetchDayData();
        List<AlertDto> alerts = alertService.generateAlerts(data.lowStockProducts(), data.expiringProducts());

        String text;
        String origin;

        if (iaProperties.isConfigured()) {
            try {
                text   = aiTextClient.generateText(SYSTEM_PROMPT, formatPrompt(data, alerts));
                origin = "IA";
            } catch (Exception e) {
                log.warn("IA no disponible, usando generador local: {}", e.getMessage());
                text   = localGenerator.generate(data, alerts);
                origin = "LOCAL";
            }
        } else {
            text   = localGenerator.generate(data, alerts);
            origin = "LOCAL";
        }

        AiSummaryRecord record = new AiSummaryRecord();
        record.setFecha(LocalDateTime.now());
        record.setText(text);
        iaRepository.save(record);

        return new SummaryResponse(LocalDateTime.now(), origin, text, alerts, data);
    }

    public List<SummaryHistoryItem> getHistory() {
        return iaRepository.findTop10ByOrderByFechaDesc().stream()
                .map(r -> new SummaryHistoryItem(r.getId(), r.getFecha(), r.getText()))
                .toList();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private DaySummaryData fetchDayData() {
        List<LowStockRow>   lowStock   = queryRepository.findLowStockProducts();
        List<ExpiryRow>     expiring   = queryRepository.findExpiringProducts();
        List<TodaySalesRow> salesRows  = queryRepository.findTodaySales();
        List<TopSellerRow>  topSellers = queryRepository.findTopSellersToday();

        long       totalSales = salesRows.stream().findFirst().map(TodaySalesRow::getTotalVentas).orElse(0L);
        BigDecimal revenue    = salesRows.stream().findFirst().map(TodaySalesRow::getMontoTotal).orElse(BigDecimal.ZERO);

        List<DaySummaryData.LowStockItem> stockItems = lowStock.stream()
                .map(r -> new DaySummaryData.LowStockItem(r.getIdProducto(), r.getNombre(),
                        r.getStock(), r.getStockMinimo(), r.getTipo()))
                .toList();

        List<DaySummaryData.ExpiryItem> expiryItems = expiring.stream()
                .map(r -> new DaySummaryData.ExpiryItem(r.getIdProducto(), r.getNombre(),
                        r.getFechaCaducidad(), r.getStock()))
                .toList();

        List<DaySummaryData.TopSellerItem> sellerItems = topSellers.stream()
                .map(r -> new DaySummaryData.TopSellerItem(r.getIdProducto(), r.getNombre(),
                        r.getUnidadesVendidas()))
                .toList();

        return new DaySummaryData(totalSales, revenue, stockItems, expiryItems, sellerItems);
    }

    private String formatPrompt(DaySummaryData data, List<AlertDto> alerts) {
        StringBuilder sb = new StringBuilder("DATOS DEL INVENTARIO HOY:\n");
        sb.append("Ventas: ").append(data.totalSalesToday())
          .append(" | Ingresos: $").append(data.totalRevenue()).append("\n");

        if (!data.topSellers().isEmpty()) {
            String sellers = data.topSellers().stream()
                    .map(ts -> ts.name() + " ×" + ts.unitsSold())
                    .collect(Collectors.joining(", "));
            sb.append("Top vendidos: ").append(sellers).append("\n");
        }

        if (!alerts.isEmpty()) {
            sb.append("Alertas (").append(alerts.size()).append("):\n");
            alerts.stream().limit(10).forEach(a ->
                sb.append("- [").append(a.level()).append("] ").append(a.title())
                  .append(": ").append(a.detail()).append("\n")
            );
        }

        return sb.toString();
    }
}
