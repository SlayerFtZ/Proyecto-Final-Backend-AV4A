package com.inventaria.api.categories.infrastructure;

import com.inventaria.api.categories.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<Category, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    @Query(value = "SELECT COUNT(*) FROM Productos WHERE IdCategoria = :categoryId", nativeQuery = true)
    int countProductsByCategoryId(@Param("categoryId") Integer categoryId);
}
