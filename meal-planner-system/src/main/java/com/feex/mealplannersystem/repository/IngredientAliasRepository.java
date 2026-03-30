package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientAliasRepository extends JpaRepository<IngredientAliasEntity, Long> {
    Optional<IngredientAliasEntity> findByRawName(String rawName);

    @Query("""
        SELECT a FROM IngredientAliasEntity a
        JOIN FETCH a.ingredient i
        LEFT JOIN FETCH i.dietaryTags dt
        LEFT JOIN FETCH dt.condition
        """)
    List<IngredientAliasEntity> findAllWithTags();
}
