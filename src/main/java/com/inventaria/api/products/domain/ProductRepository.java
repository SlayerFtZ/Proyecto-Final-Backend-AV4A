package com.inventaria.api.products.domain;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    // ── Consultas filtradas por usuario ──────────────────────────────────────

    List<Product> findAll(Integer userId, ProductFilter filter, PageRequest pageable);

    Optional<Product> findByIdAndUserId(Integer id, Integer userId);

    Optional<Product> findByBarcodeAndUserId(String barcode, Integer userId);

    boolean existsByIdAndUserId(Integer id, Integer userId);

    boolean existsByBarcodeAndUserId(String barcode, Integer userId);

    boolean existsByBarcodeAndUserIdAndIdNot(String barcode, Integer userId, Integer id);

    // ── Operaciones sobre un producto cuya propiedad ya se validó ────────────

    /** Relectura tras un UPDATE nativo; llamar solo con IDs ya validados como propios. */
    Optional<Product> findById(Integer id);

    Product save(Product product);

    void deleteById(Integer id);

    /**
     * Atomic stock decrement. Returns the number of rows updated (0 = insufficient stock).
     */
    int decrementStock(Integer productId, int quantity);

    boolean hasSales(Integer productId);
}
