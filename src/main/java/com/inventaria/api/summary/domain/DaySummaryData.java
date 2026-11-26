package com.inventaria.api.summary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DaySummaryData(
        long totalSalesToday,
        BigDecimal totalRevenue,
        List<LowStockItem> lowStockProducts,
        List<ExpiryItem> expiringProducts,
        List<TopSellerItem> topSellers
) {
    public record LowStockItem(Integer productId, String name, Integer stock, Integer minimumStock, String type) {}
    public record ExpiryItem(Integer productId, String name, LocalDate expirationDate, Integer stock) {}
    public record TopSellerItem(Integer productId, String name, long unitsSold) {}
}
