package com.inventaria.api.products.infrastructure;

import com.inventaria.api.products.domain.Product;
import com.inventaria.api.products.domain.ProductFilter;
import com.inventaria.api.products.domain.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    public ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Product> findAll(Integer userId, ProductFilter filter, PageRequest pageable) {
        return jpa.findAll(ProductSpecification.withFilter(userId, filter), pageable).getContent();
    }

    @Override
    public Optional<Product> findByIdAndUserId(Integer id, Integer userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public Optional<Product> findByBarcodeAndUserId(String barcode, Integer userId) {
        return jpa.findByBarcodeAndUserId(barcode, userId);
    }

    @Override
    public boolean existsByIdAndUserId(Integer id, Integer userId) {
        return jpa.existsByIdAndUserId(id, userId);
    }

    @Override
    public boolean existsByBarcodeAndUserId(String barcode, Integer userId) {
        return jpa.existsByBarcodeAndUserId(barcode, userId);
    }

    @Override
    public boolean existsByBarcodeAndUserIdAndIdNot(String barcode, Integer userId, Integer id) {
        return jpa.existsByBarcodeAndUserIdAndIdNot(barcode, userId, id);
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return jpa.findById(id);
    }

    @Override
    public Product save(Product product) {
        return jpa.save(product);
    }

    @Override
    public void deleteById(Integer id) {
        jpa.deleteById(id);
    }

    @Override
    public int decrementStock(Integer productId, int quantity) {
        return jpa.decrementStock(productId, quantity);
    }

    @Override
    public boolean hasSales(Integer productId) {
        return jpa.countSalesByProductId(productId) > 0;
    }
}
