package com.inventaria.api.sales.domain;

import com.inventaria.api.products.domain.Product;
import com.inventaria.api.products.domain.ProductRepository;
import com.inventaria.api.sales.infrastructure.SaleJpaRepository;
import com.inventaria.api.sales.presentation.SaleItemRequest;
import com.inventaria.api.sales.presentation.SaleItemResponse;
import com.inventaria.api.sales.presentation.SaleRequest;
import com.inventaria.api.sales.presentation.SaleResponse;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SaleService {

    private final SaleJpaRepository saleRepository;
    private final ProductRepository productRepository;
    private final CurrentUser currentUser;

    public SaleService(SaleJpaRepository saleRepository,
                       ProductRepository productRepository,
                       CurrentUser currentUser) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.currentUser = currentUser;
    }

    public List<SaleResponse> findAll(LocalDate date, int page, int size) {
        Integer userId = currentUser.getId();
        PageRequest pageReq = PageRequest.of(page, size);
        return ((date != null)
                ? saleRepository.findByFechaRangePaged(
                        userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageReq)
                : saleRepository.findAllPaged(userId, pageReq))
                .getContent().stream().map(this::toResponse).toList();
    }

    public SaleResponse findById(Integer id) {
        return saleRepository.findByIdWithItems(id, currentUser.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id));
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Integer userId = currentUser.getId();

        // Merge duplicate product IDs: sum quantities, keep first valid custom price
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        Map<Integer, BigDecimal> requestedPrices = new LinkedHashMap<>();

        for (SaleItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
            if (item.unitPrice() != null && item.unitPrice().compareTo(BigDecimal.ZERO) > 0) {
                requestedPrices.putIfAbsent(item.productId(), item.unitPrice());
            }
        }

        Sale sale = new Sale();
        sale.setUserId(userId);
        sale.setFecha(LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Integer productId = entry.getKey();
            Integer qty = entry.getValue();

            // Solo se pueden vender productos propios; uno ajeno responde 404.
            Product product = productRepository.findByIdAndUserId(productId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

            BigDecimal unitPrice = requestedPrices.getOrDefault(productId, product.getPrice());
            int snapshotStock = product.getStock();
            String productName = product.getName();

            // Atomic decrement: returns 0 if stock insufficient → triggers full rollback
            int updated = productRepository.decrementStock(productId, qty);
            if (updated == 0) {
                throw new InsufficientStockException(productName, snapshotStock, qty);
            }

            // Re-fetch to get a managed entity after the EntityManager cache was cleared
            Product freshProduct = productRepository.findById(productId).orElseThrow();

            SaleItem item = new SaleItem();
            item.setProduct(freshProduct);
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            sale.addItem(item);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }

        sale.setTotal(total);
        Sale saved = saleRepository.save(sale);

        return toResponse(saleRepository.findByIdWithItems(saved.getId(), userId).orElseThrow());
    }

    private SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new SaleResponse(sale.getId(), sale.getFecha(), sale.getTotal(), items);
    }

    private SaleItemResponse toItemResponse(SaleItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new SaleItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}
