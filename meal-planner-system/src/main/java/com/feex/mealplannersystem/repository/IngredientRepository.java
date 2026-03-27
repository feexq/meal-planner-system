package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {

    Optional<IngredientEntity> findBySlug(String slug);

    boolean existsByNormalizedName(String normalizedName);

    @Query("""
        SELECT i FROM IngredientEntity i
        WHERE (:search IS NULL OR LOWER(i.normalizedName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        AND (:available IS NULL OR i.available = :available)
        AND (:#{#categoryIds} IS NULL OR i.category.id IN :categoryIds)
    """)
    Page<IngredientEntity> findAllWithFilters(
            @Param("search") String search,
            @Param("available") Boolean available,
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable
    );
}
