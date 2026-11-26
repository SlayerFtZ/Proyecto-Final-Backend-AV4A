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
    @Operation(summary = "Todos los productos con categoría y proveedor (JOIN)")
    public List<ProductDetailRow> productDetails() {
        return service.findProductDetails();
    }

    @GetMapping("/ventas-detalle")
    @Operation(summary = "Ventas con detalle y nombre de producto (JOIN); filtro opcional desde/hasta")
    public List<SaleDetailRow> saleDetails(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.findSaleDetails(desde, hasta);
    }

    @GetMapping("/stock-bajo-por-proveedor")
    @Operation(summary = "Productos con stock ≤ stockMínimo agrupados por proveedor (JOIN)")
    public List<LowStockBySupplierRow> lowStockBySupplier() {
        return service.findLowStockBySupplier();
    }

    @GetMapping("/total-por-categoria")
    @Operation(summary = "Unidades vendidas y total por categoría (GROUP BY)")
    public List<TotalByCategoryRow> totalByCategory() {
        return service.findTotalByCategory();
    }
}
