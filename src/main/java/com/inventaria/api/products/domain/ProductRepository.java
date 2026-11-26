package com.inventaria.api.products.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll(ProductFilter filter);

    Optional<Product> findById(Integer id);

    Optional<Product> findByBarcode(String barcode);

    boolean existsById(Integer id);

    boolean existsByBarcodeAndIdNot(String barcode, Integer id);

    Product save(Product product);

    void deleteById(Integer id);

    /**
     * Atomic stock decrement. Returns the number of rows updated (0 = insufficient stock).
     */
    int decrementStock(Integer productId, int quantity);

    boolean hasSales(Integer productId);
}
