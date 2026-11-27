package com.inventaria.api.categories.infrastructure;

import com.inventaria.api.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<Category, Integer> {

    // ── Consultas filtradas por usuario ──────────────────────────────────────

    List<Category> findAllByUserId(Integer userId);

    Optional<Category> findByIdAndUserId(Integer id, Integer userId);

    boolean existsByIdAndUserId(Integer id, Integer userId);

    boolean existsByUserIdAndNameIgnoreCase(Integer userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Integer userId, String name, Integer id);

    // La categoría ya se validó como propia antes de llamar a este método.
    @Query(value = "SELECT COUNT(*) FROM Productos WHERE IdCategoria = :categoryId", nativeQuery = true)
    int countProductsByCategoryId(@Param("categoryId") Integer categoryId);
}
