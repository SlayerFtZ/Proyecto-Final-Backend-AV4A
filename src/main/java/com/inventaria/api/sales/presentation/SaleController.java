package com.inventaria.api.sales.presentation;

import com.inventaria.api.sales.domain.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Sales", description = "Registro y consulta de ventas")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar ventas (filtro opcional: fecha=YYYY-MM-DD)")
    public List<SaleResponse> findAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.findAll(fecha);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta con detalle y nombres de producto")
    public SaleResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Registrar venta y descontar stock (transacción atómica)")
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        SaleResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
