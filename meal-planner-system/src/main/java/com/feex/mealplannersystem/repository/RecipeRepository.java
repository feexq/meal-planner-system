package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {

    Optional<RecipeEntity> findBySlug(String slug);

        @Query("""
        SELECT DISTINCT r FROM RecipeEntity r
        LEFT JOIN r.tags t
        WHERE (:search IS NULL OR LOWER(r.name) LIKE :search)
        AND (:mealType IS NULL OR r.mealType = :mealType)
        AND (:cookTime IS NULL OR r.cookTime = :cookTime)
        AND (:cookComplexity IS NULL OR r.cookComplexity = :cookComplexity)
        AND (:cookBudget IS NULL OR r.cookBudget = :cookBudget)
        AND (:tag IS NULL OR t.name = :tag)
    """)
    Page<RecipeEntity> findAllWithFilters(
            @Param("search") String search,
            @Param("mealType") MealType mealType,
            @Param("cookTime") CookTime cookTime,
            @Param("cookComplexity") CookComplexity cookComplexity,
            @Param("cookBudget") CookBudget cookBudget,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT r FROM RecipeEntity r
            LEFT JOIN FETCH r.nutrition
            LEFT JOIN FETCH r.tags
            LEFT JOIN FETCH r.ingredients
            """)
    List<RecipeEntity> findAllForGenerator();

    @Query("""
        SELECT DISTINCT r FROM RecipeEntity r
        JOIN r.ingredients ri
        WHERE ri.ingredient.id = :ingredientId
    """)
    Page<RecipeEntity> findByIngredientId(
            @Param("ingredientId") Long ingredientId,
            Pageable pageable
    );
}
