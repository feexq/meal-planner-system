package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientAliasRepository extends JpaRepository<IngredientAliasEntity, Long> {
    Optional<IngredientAliasEntity> findByRawName(String rawName);
}
