package com.inventaria.api.reports.presentation;

import com.inventaria.api.reports.domain.LowStockBySupplierRow;
import com.inventaria.api.reports.domain.ProductDetailRow;
import com.inventaria.api.reports.domain.ReportService;
import com.inventaria.api.reports.domain.SaleDetailRow;
import com.inventaria.api.reports.domain.TotalByCategoryRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reports", description = "Consultas JOIN y GROUP BY para análisis de inventario")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/productos-detalle")
    @Operation(summary = "Todos los productos con categoría y proveedor (JOIN). Paginación: page (0-based), size (default 50)")
    public List<ProductDetailRow> productDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.findProductDetails(page, size);
    }

    @GetMapping("/ventas-detalle")
    @Operation(summary = "Ventas con detalle y nombre de producto (JOIN); filtro opcional desde/hasta. Paginación: page, size (default 50)")
    public List<SaleDetailRow> saleDetails(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.findSaleDetails(desde, hasta, page, size);
    }

    @GetMapping("/stock-bajo-por-proveedor")
    @Operation(summary = "Productos con stock ≤ stockMínimo agrupados por proveedor (JOIN). Paginación: page, size (default 50)")
    public List<LowStockBySupplierRow> lowStockBySupplier(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.findLowStockBySupplier(page, size);
    }

    @GetMapping("/total-por-categoria")
    @Operation(summary = "Unidades vendidas y total por categoría (GROUP BY)")
    public List<TotalByCategoryRow> totalByCategory() {
        return service.findTotalByCategory();
    }
}
