package com.inventaria.api.products.domain;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.products.presentation.*;
import com.inventaria.api.shared.error.DuplicateBarcodeException;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
import com.inventaria.api.suppliers.domain.Supplier;
import com.inventaria.api.suppliers.infrastructure.SupplierJpaRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final CurrentUser currentUser;

    public ProductService(ProductRepository productRepository,
                          CategoryJpaRepository categoryRepository,
                          SupplierJpaRepository supplierRepository,
                          ProductMapper mapper,
                          Validator validator,
                          CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
        this.validator = validator;
        this.currentUser = currentUser;
    }

    private static final Sort PRODUCT_SORT = Sort.by(Sort.Direction.ASC, "name");

    public List<ProductResponse> findAll(String query, Integer categoryId,
                                          Integer supplierId, ProductType type, int page, int size) {
        PageRequest pageReq = PageRequest.of(page, size, PRODUCT_SORT);
        return productRepository.findAll(currentUser.getId(),
                        new ProductFilter(query, categoryId, supplierId, type), pageReq)
                .stream().map(mapper::toResponse).toList();
    }

    public ProductResponse findById(Integer id) {
        return mapper.toResponse(getOwned(id, currentUser.getId()));
    }

    public ProductResponse findByBarcode(String barcode) {
        return productRepository.findByBarcodeAndUserId(barcode, currentUser.getId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con código de barras '" + barcode + "' no fue encontrado"));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Integer userId = currentUser.getId();

        if (productRepository.existsByBarcodeAndUserId(request.barcode(), userId)) {
            throw new DuplicateBarcodeException(request.barcode());
        }

        Category category = getOwnedCategory(request.categoryId(), userId);
        Supplier supplier = getOwnedSupplier(request.supplierId(), userId);

        Product product = new Product();
        product.setUserId(userId);
        mapper.applyRequest(product, request, category, supplier);
        validateCrossType(product);

        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Integer userId = currentUser.getId();
        Product product = getOwned(id, userId);

        if (productRepository.existsByBarcodeAndUserIdAndIdNot(request.barcode(), userId, id)) {
            throw new DuplicateBarcodeException(request.barcode());
        }

        Category category = getOwnedCategory(request.categoryId(), userId);
        Supplier supplier = getOwnedSupplier(request.supplierId(), userId);

        mapper.applyRequest(product, request, category, supplier);
        validateCrossType(product);

        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Integer id) {
        if (!productRepository.existsByIdAndUserId(id, currentUser.getId())) {
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

        Product product = getOwned(id, currentUser.getId());

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
        Integer userId = currentUser.getId();
        List<BatchErrorItem> errors = new ArrayList<>();
        int inserted = 0;
        List<ProductRequest> products = request.products();

        for (int i = 0; i < products.size(); i++) {
            int row = i + 1;
            ProductRequest req = products.get(i);

            String preError = validateForBatch(req, userId);
            if (preError != null) {
                errors.add(new BatchErrorItem(row, req.barcode(), preError));
                continue;
            }

            try {
                Category category = categoryRepository.findByIdAndUserId(req.categoryId(), userId).orElseThrow();
                Supplier supplier = supplierRepository.findByIdAndUserId(req.supplierId(), userId).orElseThrow();

                Product product = new Product();
                product.setUserId(userId);
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

    // ── private helpers ───────────────────────────────────────────────────────

    /** Devuelve el producto solo si pertenece al usuario; si no, 404. */
    private Product getOwned(Integer id, Integer userId) {
        return productRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    /** La categoría debe ser del mismo usuario que el producto. */
    private Category getOwnedCategory(Integer categoryId, Integer userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoryId));
    }

    /** El proveedor debe ser del mismo usuario que el producto. */
    private Supplier getOwnedSupplier(Integer supplierId, Integer userId) {
        return supplierRepository.findByIdAndUserId(supplierId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", supplierId));
    }

    private String validateForBatch(ProductRequest req, Integer userId) {
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
        }
        if (req.barcode() != null && productRepository.existsByBarcodeAndUserId(req.barcode(), userId)) {
            return "Código de barras '" + req.barcode() + "' ya está registrado";
        }
        if (req.categoryId() != null && !categoryRepository.existsByIdAndUserId(req.categoryId(), userId)) {
            return "Categoría con id " + req.categoryId() + " no existe";
        }
        if (req.supplierId() != null && !supplierRepository.existsByIdAndUserId(req.supplierId(), userId)) {
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
