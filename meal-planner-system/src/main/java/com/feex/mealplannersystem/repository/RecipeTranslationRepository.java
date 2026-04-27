package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.recipe.RecipeTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslationEntity, Long> {
    Optional<RecipeTranslationEntity> findByRecipeIdAndLanguageCode(Long recipeId, String languageCode);
    List<RecipeTranslationEntity> findByRecipeIdInAndLanguageCode(
            Collection<Long> recipeIds, String languageCode
    );
}