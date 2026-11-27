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

/** Todas las consultas filtran por el usuario dueño de la venta. */
public interface SaleJpaRepository extends JpaRepository<Sale, Integer> {

    @Query("""
            SELECT DISTINCT s FROM Sale s
            LEFT JOIN FETCH s.items i
            LEFT JOIN FETCH i.product
            WHERE s.userId = :userId
            ORDER BY s.fecha DESC
            """)
    List<Sale> findAllWithItems(@Param("userId") Integer userId);

    @Query("""
            SELECT DISTINCT s FROM Sale s
            LEFT JOIN FETCH s.items i
            LEFT JOIN FETCH i.product
            WHERE s.userId = :userId
              AND s.fecha >= :start AND s.fecha < :end
            ORDER BY s.fecha DESC
            """)
    List<Sale> findByFechaRangeWithItems(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.items i
            LEFT JOIN FETCH i.product
            WHERE s.id = :id AND s.userId = :userId
            """)
    Optional<Sale> findByIdWithItems(@Param("id") Integer id, @Param("userId") Integer userId);

    // ── Paginated queries (lazy loading within @Transactional service) ────────

    @Query(value = "SELECT s FROM Sale s WHERE s.userId = :userId ORDER BY s.fecha DESC",
           countQuery = "SELECT COUNT(s) FROM Sale s WHERE s.userId = :userId")
    Page<Sale> findAllPaged(@Param("userId") Integer userId, Pageable pageable);

    @Query(value = """
            SELECT s FROM Sale s
            WHERE s.userId = :userId
              AND s.fecha >= :start AND s.fecha < :end
            ORDER BY s.fecha DESC
            """,
           countQuery = """
            SELECT COUNT(s) FROM Sale s
            WHERE s.userId = :userId
              AND s.fecha >= :start AND s.fecha < :end
            """)
    Page<Sale> findByFechaRangePaged(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
