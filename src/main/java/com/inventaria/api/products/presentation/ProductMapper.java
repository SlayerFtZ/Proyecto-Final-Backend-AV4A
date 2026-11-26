package com.inventaria.api.products.presentation;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.products.domain.Product;
import com.inventaria.api.suppliers.domain.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getMinimumStock(),
                product.getType(),
                product.getExpirationDate(),
                product.getWarrantyMonths(),
                product.getSize(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSupplier().getId(),
                product.getSupplier().getName()
        );
    }

    public void applyRequest(Product product, ProductRequest request,
                              Category category, Supplier supplier) {
        product.setBarcode(request.barcode());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setMinimumStock(request.minimumStock());
        product.setType(request.type());
        product.setExpirationDate(request.expirationDate());
        product.setWarrantyMonths(request.warrantyMonths());
        product.setSize(request.size());
        product.setCategory(category);
        product.setSupplier(supplier);
    }
}
