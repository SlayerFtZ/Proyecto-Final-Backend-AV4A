package com.inventaria.api.suppliers.domain;

import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
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
    private final CurrentUser currentUser;

    public SupplierService(SupplierJpaRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public List<SupplierResponse> findAll() {
        return repository.findAllByUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public SupplierResponse findById(Integer id) {
        return toResponse(getOwned(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setUserId(currentUser.getId());
        applyRequest(supplier, request);
        return toResponse(repository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Integer id, SupplierRequest request) {
        Supplier supplier = getOwned(id);
        applyRequest(supplier, request);
        return toResponse(repository.save(supplier));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsByIdAndUserId(id, currentUser.getId())) {
            throw new ResourceNotFoundException("Proveedor", id);
        }
        if (repository.countProductsBySupplierId(id) > 0) {
            throw new RecordInUseException("proveedor", id);
        }
        repository.deleteById(id);
    }

    /** Devuelve el proveedor solo si pertenece al usuario actual; si no, 404. */
    private Supplier getOwned(Integer id) {
        return repository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
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
