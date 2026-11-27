package com.inventaria.api.sales.domain;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.products.domain.Product;
import com.inventaria.api.products.domain.ProductRepository;
import com.inventaria.api.sales.infrastructure.SaleJpaRepository;
import com.inventaria.api.sales.presentation.SaleItemRequest;
import com.inventaria.api.sales.presentation.SaleRequest;
import com.inventaria.api.sales.presentation.SaleResponse;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
import com.inventaria.api.suppliers.domain.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock SaleJpaRepository saleRepository;
    @Mock ProductRepository productRepository;
    @Mock CurrentUser currentUser;

    @InjectMocks SaleService service;

    private static final Integer USER_ID = 1;

    @Test
    void create_sufficientStock_returnsSaleResponse() {
        Product product = buildProduct(1, "Leche", 10, new BigDecimal("5.00"));

        when(currentUser.getId()).thenReturn(USER_ID);
        when(productRepository.findByIdAndUserId(1, USER_ID))
                .thenReturn(Optional.of(product));  // snapshot + price (valida propiedad)
        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));  // re-fetch after cache clear
        when(productRepository.decrementStock(1, 2)).thenReturn(1);

        Sale mockSale = mock(Sale.class);
        SaleItem mockItem = mock(SaleItem.class);

        when(mockSale.getId()).thenReturn(100);
        when(mockSale.getFecha()).thenReturn(LocalDateTime.now());
        when(mockSale.getTotal()).thenReturn(new BigDecimal("10.00"));
        when(mockSale.getItems()).thenReturn(List.of(mockItem));
        when(mockItem.getId()).thenReturn(1);
        when(mockItem.getProduct()).thenReturn(product);
        when(mockItem.getQuantity()).thenReturn(2);
        when(mockItem.getUnitPrice()).thenReturn(new BigDecimal("5.00"));

        when(saleRepository.save(any())).thenReturn(mockSale);
        when(saleRepository.findByIdWithItems(100, USER_ID)).thenReturn(Optional.of(mockSale));

        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1, 2, null)));
        SaleResponse response = service.create(request);

        assertNotNull(response);
        assertEquals(100, response.id());
        assertEquals(new BigDecimal("10.00"), response.total());
        verify(productRepository).decrementStock(1, 2);
        verify(saleRepository).save(argThat((Sale s) -> USER_ID.equals(s.getUserId())));
    }

    @Test
    void create_insufficientStock_throwsInsufficientStockException() {
        Product product = buildProduct(1, "Leche", 2, new BigDecimal("5.00"));

        when(currentUser.getId()).thenReturn(USER_ID);
        when(productRepository.findByIdAndUserId(1, USER_ID)).thenReturn(Optional.of(product));
        when(productRepository.decrementStock(1, 5)).thenReturn(0);

        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(1, 5, null)));

        assertThrows(InsufficientStockException.class, () -> service.create(request));
        verify(saleRepository, never()).save(any());
    }

    @Test
    void create_productOfAnotherUser_throwsNotFound() {
        // El producto 7 existe, pero no es del usuario: findByIdAndUserId no lo encuentra.
        when(currentUser.getId()).thenReturn(USER_ID);
        when(productRepository.findByIdAndUserId(7, USER_ID)).thenReturn(Optional.empty());

        SaleRequest request = new SaleRequest(List.of(new SaleItemRequest(7, 1, null)));

        assertThrows(ResourceNotFoundException.class, () -> service.create(request));
        verify(productRepository, never()).decrementStock(anyInt(), anyInt());
        verify(saleRepository, never()).save(any());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Product buildProduct(int id, String name, int stock, BigDecimal price) {
        Category category = new Category();
        category.setName("Test");
        Supplier supplier = new Supplier();
        supplier.setName("Test");

        Product p = new Product();
        p.setName(name);
        p.setStock(stock);
        p.setPrice(price);
        p.setCategory(category);
        p.setSupplier(supplier);
        return p;
    }
}
