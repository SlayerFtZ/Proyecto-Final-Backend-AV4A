package com.inventaria.api.suppliers.infrastructure;

import com.inventaria.api.suppliers.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierJpaRepository extends JpaRepository<Supplier, Integer> {

    @Query(value = "SELECT COUNT(*) FROM Productos WHERE IdProveedor = :supplierId", nativeQuery = true)
    int countProductsBySupplierId(@Param("supplierId") Integer supplierId);
}
