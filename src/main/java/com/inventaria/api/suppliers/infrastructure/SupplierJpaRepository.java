package com.inventaria.api.suppliers.infrastructure;

import com.inventaria.api.suppliers.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierJpaRepository extends JpaRepository<Supplier, Integer> {

    // ── Consultas filtradas por usuario ──────────────────────────────────────

    List<Supplier> findAllByUserId(Integer userId);

    Optional<Supplier> findByIdAndUserId(Integer id, Integer userId);

    boolean existsByIdAndUserId(Integer id, Integer userId);

    // El proveedor ya se validó como propio antes de llamar a este método.
    @Query(value = "SELECT COUNT(*) FROM Productos WHERE IdProveedor = :supplierId", nativeQuery = true)
    int countProductsBySupplierId(@Param("supplierId") Integer supplierId);
}
