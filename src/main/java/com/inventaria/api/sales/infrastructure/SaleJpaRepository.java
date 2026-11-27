package com.inventaria.api.sales.infrastructure;

import com.inventaria.api.sales.domain.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleJpaRepository extends JpaRepository<Sale, Integer> {

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.product ORDER BY s.fecha DESC")
    List<Sale> findAllWithItems();

    @Query("""
            SELECT DISTINCT s FROM Sale s
            LEFT JOIN FETCH s.items i
            LEFT JOIN FETCH i.product
            WHERE s.fecha >= :start AND s.fecha < :end
            ORDER BY s.fecha DESC
            """)
    List<Sale> findByFechaRangeWithItems(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.product WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") Integer id);

    // ── Paginated queries (lazy loading within @Transactional service) ────────

    @Query(value = "SELECT s FROM Sale s ORDER BY s.fecha DESC",
           countQuery = "SELECT COUNT(s) FROM Sale s")
    Page<Sale> findAllPaged(Pageable pageable);

    @Query(value = "SELECT s FROM Sale s WHERE s.fecha >= :start AND s.fecha < :end ORDER BY s.fecha DESC",
           countQuery = "SELECT COUNT(s) FROM Sale s WHERE s.fecha >= :start AND s.fecha < :end")
    Page<Sale> findByFechaRangePaged(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
