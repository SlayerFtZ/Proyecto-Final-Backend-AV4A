package com.inventaria.api.products.domain;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.products.presentation.*;
import com.inventaria.api.suppliers.domain.Supplier;
import com.inventaria.api.suppliers.infrastructure.SupplierJpaRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryJpaRepository categoryRepository;
    @Mock SupplierJpaRepository supplierRepository;
    @Mock ProductMapper mapper;
    @Mock Validator validator;

    @InjectMocks ProductService service;

    private Category category;
    private Supplier supplier;
    private ProductResponse mockResponse;

    @BeforeEach
    void setUp() {
        category = new Category();
        supplier = new Supplier();
        mockResponse = new ProductResponse(1, "12345678901", "Test", null,
                new BigDecimal("10.00"), 10, 5, ProductType.PERISHABLE,
                LocalDate.now().plusDays(30), null, null, 1, "Cat", 1, "Sup");

        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
        when(supplierRepository.findById(anyInt())).thenReturn(Optional.of(supplier));
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mockResponse);
    }

    // ── PERISHABLE ───────────────────────────────────────────────────────────

    @Test
    void create_perishable_withExpirationDate_savesSuccessfully() {
        applyMapper(ProductType.PERISHABLE, p -> {
            p.setExpirationDate(LocalDate.now().plusDays(10));
        });

        assertDoesNotThrow(() -> service.create(perishableRequest(LocalDate.now().plusDays(10))));
        verify(productRepository).save(any());
    }

    @Test
    void create_perishable_missingExpirationDate_throwsTypeValidationException() {
        applyMapper(ProductType.PERISHABLE, p -> { /* expirationDate stays null */ });

        assertThrows(TypeValidationException.class,
                () -> service.create(perishableRequest(null)));
    }

    // ── ELECTRONIC ───────────────────────────────────────────────────────────

    @Test
    void create_electronic_withWarrantyMonths_savesSuccessfully() {
        applyMapper(ProductType.ELECTRONIC, p -> p.setWarrantyMonths(12));

        assertDoesNotThrow(() -> service.create(electronicRequest(12)));
        verify(productRepository).save(any());
    }

    @Test
    void create_electronic_missingWarrantyMonths_throwsTypeValidationException() {
        applyMapper(ProductType.ELECTRONIC, p -> { /* warrantyMonths stays null */ });

        assertThrows(TypeValidationException.class,
                () -> service.create(electronicRequest(null)));
    }

    // ── CLOTHING ─────────────────────────────────────────────────────────────

    @Test
    void create_clothing_withSize_savesSuccessfully() {
        applyMapper(ProductType.CLOTHING, p -> p.setSize("M"));

        assertDoesNotThrow(() -> service.create(clothingRequest("M")));
        verify(productRepository).save(any());
    }

    @Test
    void create_clothing_missingSize_throwsTypeValidationException() {
        applyMapper(ProductType.CLOTHING, p -> { /* size stays null */ });

        assertThrows(TypeValidationException.class,
                () -> service.create(clothingRequest(null)));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void applyMapper(ProductType type, java.util.function.Consumer<Product> customizer) {
        doAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setType(type);
            customizer.accept(p);
            p.setCategory(category);
            p.setSupplier(supplier);
            return null;
        }).when(mapper).applyRequest(any(), any(), any(), any());
    }

    private ProductRequest perishableRequest(LocalDate expirationDate) {
        return new ProductRequest("12345678901", "Leche", null,
                new BigDecimal("10.00"), 10, 5,
                ProductType.PERISHABLE, expirationDate, null, null, 1, 1);
    }

    private ProductRequest electronicRequest(Integer warrantyMonths) {
        return new ProductRequest("12345678902", "Laptop", null,
                new BigDecimal("500.00"), 5, 2,
                ProductType.ELECTRONIC, null, warrantyMonths, null, 1, 1);
    }

    private ProductRequest clothingRequest(String size) {
        return new ProductRequest("12345678903", "Camiseta", null,
                new BigDecimal("15.00"), 20, 5,
                ProductType.CLOTHING, null, null, size, 1, 1);
    }
}
