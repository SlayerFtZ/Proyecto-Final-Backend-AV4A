package com.inventaria.api.products.infrastructure;

import com.inventaria.api.products.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository
        extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE Productos
               SET Stock = Stock - :quantity
             WHERE IdProducto = :productId
               AND Stock >= :quantity
            """, nativeQuery = true)
    int decrementStock(@Param("productId") Integer productId, @Param("quantity") int quantity);

    @Query(value = "SELECT COUNT(*) FROM DetalleVenta WHERE IdProducto = :productId", nativeQuery = true)
    int countSalesByProductId(@Param("productId") Integer productId);
}
