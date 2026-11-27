package com.inventaria.api.reports.domain;

import com.inventaria.api.reports.infrastructure.ReportQueryRepository;
import org.springframework.data.domain.PageRequest;
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

    public List<ProductDetailRow> findProductDetails(int page, int size) {
        return repository.findProductDetails(PageRequest.of(page, size)).getContent();
    }

    public List<SaleDetailRow> findSaleDetails(LocalDate desde, LocalDate hasta, int page, int size) {
        LocalDateTime start = (desde != null ? desde : DEFAULT_FROM).atStartOfDay();
        LocalDateTime end = (hasta != null ? hasta.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay();
        return repository.findSaleDetails(start, end, PageRequest.of(page, size)).getContent();
    }

    public List<LowStockBySupplierRow> findLowStockBySupplier(int page, int size) {
        return repository.findLowStockBySupplier(PageRequest.of(page, size)).getContent();
    }

    public List<TotalByCategoryRow> findTotalByCategory() {
        return repository.findTotalByCategory();
    }
}
