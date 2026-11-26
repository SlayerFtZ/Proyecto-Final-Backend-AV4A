package com.inventaria.api.reports.domain;

import com.inventaria.api.reports.infrastructure.ReportQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final LocalDate DEFAULT_FROM = LocalDate.of(1900, 1, 1);

    private final ReportQueryRepository repository;

    public ReportService(ReportQueryRepository repository) {
        this.repository = repository;
    }

    public List<ProductDetailRow> findProductDetails() {
        return repository.findProductDetails();
    }

    public List<SaleDetailRow> findSaleDetails(LocalDate desde, LocalDate hasta) {
        LocalDateTime start = (desde != null ? desde : DEFAULT_FROM).atStartOfDay();
        // hasta is exclusive upper bound: include the full last day
        LocalDateTime end = (hasta != null ? hasta.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return repository.findSaleDetails(start, end);
    }

    public List<LowStockBySupplierRow> findLowStockBySupplier() {
        return repository.findLowStockBySupplier();
    }

    public List<TotalByCategoryRow> findTotalByCategory() {
        return repository.findTotalByCategory();
    }
}
