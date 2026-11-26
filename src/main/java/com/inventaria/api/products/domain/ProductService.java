package com.inventaria.api.products.domain;

import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.shared.error.DuplicateBarcodeException;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.suppliers.infrastructure.SupplierJpaRepository;
import com.inventaria.api.products.presentation.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryJpaRepository categoryRepository;
    private final SupplierJpaRepository supplierRepository;
    private final ProductMapper mapper;
    private final Validator validator;

    public ProductService(ProductRepository productRepository,
                          CategoryJpaRepository categoryRepository,
                          SupplierJpaRepository supplierRepository,
                          ProductMapper mapper,
                          Validator validator) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
        this.validator = validator;
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

    @Transactional
    public BatchResponse batchImport(BatchRequest request) {
        List<BatchErrorItem> errors = new ArrayList<>();
        int inserted = 0;
        List<ProductRequest> products = request.products();

        for (int i = 0; i < products.size(); i++) {
            int row = i + 1;
            ProductRequest req = products.get(i);

            String preError = validateForBatch(req);
            if (preError != null) {
                errors.add(new BatchErrorItem(row, req.barcode(), preError));
                continue;
            }

            try {
                var category = categoryRepository.findById(req.categoryId()).orElseThrow();
                var supplier = supplierRepository.findById(req.supplierId()).orElseThrow();

                Product product = new Product();
                mapper.applyRequest(product, req, category, supplier);
                validateCrossType(product);
                productRepository.save(product);
                inserted++;
            } catch (TypeValidationException e) {
                errors.add(new BatchErrorItem(row, req.barcode(), e.getMessage()));
            } catch (Exception e) {
                errors.add(new BatchErrorItem(row, req.barcode(), "Error al guardar: " + e.getMessage()));
            }
        }

        return new BatchResponse(products.size(), inserted, errors);
    }

    private String validateForBatch(ProductRequest req) {
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
        }
        if (req.barcode() != null && productRepository.findByBarcode(req.barcode()).isPresent()) {
            return "Código de barras '" + req.barcode() + "' ya está registrado";
        }
        if (req.categoryId() != null && !categoryRepository.existsById(req.categoryId())) {
            return "Categoría con id " + req.categoryId() + " no existe";
        }
        if (req.supplierId() != null && !supplierRepository.existsById(req.supplierId())) {
            return "Proveedor con id " + req.supplierId() + " no existe";
        }
        return null;
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
