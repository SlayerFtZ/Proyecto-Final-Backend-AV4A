package com.inventaria.api.products.presentation;

import com.inventaria.api.products.domain.ProductService;
import com.inventaria.api.products.domain.ProductType;
import com.inventaria.api.products.domain.TypeValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Products", description = "CRUD de productos con gestión de inventario")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos con filtros opcionales: q, categoryId, supplierId, type. Paginación: page (0-based), size (default 20)")
    public List<ProductResponse> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductType productType = parseType(type);
        return service.findAll(q, categoryId, supplierId, productType, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public ProductResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @GetMapping("/codigo/{barcode}")
    @Operation(summary = "Buscar producto por código de barras (para escáner)")
    public ProductResponse findByBarcode(@PathVariable String barcode) {
        return service.findByBarcode(barcode);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public ProductResponse update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Ajustar stock (delta > 0 = añadir, delta < 0 = reducir)")
    public ProductResponse adjustStock(@PathVariable Integer id,
                                        @Valid @RequestBody StockRequest request) {
        return service.adjustStock(id, request.delta());
    }

    @PostMapping("/lote")
    @Operation(summary = "Importar lote de productos (máx. 500); errores por fila sin abortar el lote completo")
    public BatchResponse batchImport(@Valid @RequestBody BatchRequest request) {
        return service.batchImport(request);
    }

    private ProductType parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return ProductType.fromDbValue(type);
        } catch (IllegalArgumentException e) {
            throw new TypeValidationException(e.getMessage());
        }
    }
}
