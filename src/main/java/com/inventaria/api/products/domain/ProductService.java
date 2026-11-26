package com.inventaria.api.products.domain;

import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.shared.error.DuplicateBarcodeException;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.suppliers.infrastructure.SupplierJpaRepository;
import com.inventaria.api.products.presentation.ProductMapper;
import com.inventaria.api.products.presentation.ProductRequest;
import com.inventaria.api.products.presentation.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryJpaRepository categoryRepository;
    private final SupplierJpaRepository supplierRepository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository,
                          CategoryJpaRepository categoryRepository,
                          SupplierJpaRepository supplierRepository,
                          ProductMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
    }

    public List<ProductResponse> findAll(String query, Integer categoryId,
                                          Integer supplierId, ProductType type) {
        return productRepository.findAll(new ProductFilter(query, categoryId, supplierId, type))
                .stream().map(mapper::toResponse).toList();
    }

    public ProductResponse findById(Integer id) {
        return productRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    public ProductResponse findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con código de barras '" + barcode + "' no fue encontrado"));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.findByBarcode(request.barcode()).isPresent()) {
            throw new DuplicateBarcodeException(request.barcode());
        }

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", request.categoryId()));
        var supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.supplierId()));

        Product product = new Product();
        mapper.applyRequest(product, request, category, supplier);
        validateCrossType(product);

        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        if (productRepository.existsByBarcodeAndIdNot(request.barcode(), id)) {
            throw new DuplicateBarcodeException(request.barcode());
        }

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", request.categoryId()));
        var supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.supplierId()));

        mapper.applyRequest(product, request, category, supplier);
        validateCrossType(product);

        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto", id);
        }
        if (productRepository.hasSales(id)) {
            throw new RecordInUseException("producto", id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse adjustStock(Integer id, Integer delta) {
        if (delta == 0) {
            throw new TypeValidationException("El delta de stock no puede ser 0");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        if (delta < 0) {
            int updated = productRepository.decrementStock(id, Math.abs(delta));
            if (updated == 0) {
                throw new InsufficientStockException(product.getName(), product.getStock(), Math.abs(delta));
            }
            // clearAutomatically = true in @Modifying ensures fresh read
            return productRepository.findById(id).map(mapper::toResponse).orElseThrow();
        }

        product.setStock(product.getStock() + delta);
        return mapper.toResponse(productRepository.save(product));
    }

    private void validateCrossType(Product product) {
        switch (product.getType()) {
            case PERISHABLE -> {
                if (product.getExpirationDate() == null) {
                    throw new TypeValidationException(
                            "La fecha de caducidad (expirationDate) es obligatoria para productos Perecederos");
                }
                product.setWarrantyMonths(null);
                product.setSize(null);
            }
            case ELECTRONIC -> {
                if (product.getWarrantyMonths() == null) {
                    throw new TypeValidationException(
                            "Los meses de garantía (warrantyMonths) son obligatorios para productos Electrónicos");
                }
                product.setExpirationDate(null);
                product.setSize(null);
            }
            case CLOTHING -> {
                if (product.getSize() == null || product.getSize().isBlank()) {
                    throw new TypeValidationException(
                            "La talla (size) es obligatoria para productos de Ropa");
                }
                product.setExpirationDate(null);
                product.setWarrantyMonths(null);
            }
        }
    }
}
