package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameUk(String nameUk);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE (:search IS NULL OR LOWER(p.nameUk) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        AND (:available IS NULL OR p.available = :available)
        AND (:#{#categoryIds} IS NULL OR p.category.id IN :categoryIds)
    """)
    Page<ProductEntity> findAllWithFilters(
            @Param("search") String search,
            @Param("available") Boolean available,
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM ProductEntity p
        LEFT JOIN FETCH p.tags t
        LEFT JOIN FETCH p.category c
        WHERE p.id = :id
    """)
    Optional<ProductEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM ProductEntity p JOIN IngredientEntity i ON i.product.id = p.id WHERE i.id IN :ingredientIds")
    List<ProductEntity> findAllByIngredientIds(@Param("ingredientIds") List<Long> ingredientIds);
}