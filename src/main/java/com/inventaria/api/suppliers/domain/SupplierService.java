package com.inventaria.api.suppliers.domain;

import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.suppliers.infrastructure.SupplierJpaRepository;
import com.inventaria.api.suppliers.presentation.SupplierRequest;
import com.inventaria.api.suppliers.presentation.SupplierResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierJpaRepository repository;

    public SupplierService(SupplierJpaRepository repository) {
        this.repository = repository;
    }

    public List<SupplierResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public SupplierResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        return toResponse(repository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Integer id, SupplierRequest request) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
        applyRequest(supplier, request);
        return toResponse(repository.save(supplier));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Proveedor", id);
        }
        if (repository.countProductsBySupplierId(id) > 0) {
            throw new RecordInUseException("proveedor", id);
        }
        repository.deleteById(id);
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
    }

    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getEmail());
    }
}
