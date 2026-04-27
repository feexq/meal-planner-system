package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.projection.RecipeMatchProjection;
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

    @Query(
            value = """
            SELECT r FROM RecipeEntity r
            LEFT JOIN FETCH r.nutrition
            WHERE (:rawSearch IS NULL 
                   OR LOWER(r.name) LIKE :likeSearch 
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND LOWER(t.name) LIKE :likeSearch
                   )
                   OR function('word_similarity', CAST(:rawSearch AS string), LOWER(r.name)) > 0.4
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND function('word_similarity', CAST(:rawSearch AS string), LOWER(t.name)) > 0.4
                   ))
            AND (:mealType IS NULL OR r.mealType = :mealType)
            AND (:cookTime IS NULL OR r.cookTime = :cookTime)
            AND (:cookComplexity IS NULL OR r.cookComplexity = :cookComplexity)
            AND (:cookBudget IS NULL OR r.cookBudget = :cookBudget)
            AND (:tag IS NULL OR EXISTS (SELECT 1 FROM r.tags t WHERE t.name = :tag))
        """,
            countQuery = """
            SELECT COUNT(r.id) FROM RecipeEntity r
            WHERE (:rawSearch IS NULL 
                   OR LOWER(r.name) LIKE :likeSearch 
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND LOWER(t.name) LIKE :likeSearch
                   )
                   OR function('word_similarity', CAST(:rawSearch AS string), LOWER(r.name)) > 0.4
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND function('word_similarity', CAST(:rawSearch AS string), LOWER(t.name)) > 0.4
                   ))
            AND (:mealType IS NULL OR r.mealType = :mealType)
            AND (:cookTime IS NULL OR r.cookTime = :cookTime)
            AND (:cookComplexity IS NULL OR r.cookComplexity = :cookComplexity)
            AND (:cookBudget IS NULL OR r.cookBudget = :cookBudget)
            AND (:tag IS NULL OR EXISTS (SELECT 1 FROM r.tags t WHERE t.name = :tag))
        """
    )
    Page<RecipeEntity> findAllWithFilters(
            @Param("rawSearch") String rawSearch,
            @Param("likeSearch") String likeSearch,
            @Param("mealType") MealType mealType,
            @Param("cookTime") CookTime cookTime,
            @Param("cookComplexity") CookComplexity cookComplexity,
            @Param("cookBudget") CookBudget cookBudget,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT DISTINCT r FROM RecipeEntity r
            LEFT JOIN FETCH r.nutrition
            WHERE (:rawSearch IS NULL 
                   OR LOWER(r.name) LIKE :likeSearch 
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND LOWER(t.name) LIKE :likeSearch
                   )
                   OR function('word_similarity', CAST(:rawSearch AS string), LOWER(r.name)) > 0.4
                   OR EXISTS (
                       SELECT 1 FROM RecipeTranslationEntity t 
                       WHERE t.recipe = r AND function('word_similarity', CAST(:rawSearch AS string), LOWER(t.name)) > 0.4
                   ))
            AND (:mealTypes IS NULL OR r.mealType IN :mealTypes)
            AND (:cookTimes IS NULL OR r.cookTime IN :cookTimes)
            AND (:cookComplexities IS NULL OR r.cookComplexity IN :cookComplexities)
            AND (:cookBudgets IS NULL OR r.cookBudget IN :cookBudgets)
            AND (:tags IS NULL OR EXISTS (SELECT 1 FROM r.tags t WHERE t.name IN :tags))
        """
    )
    Page<RecipeEntity> findForMarketplace(
            @Param("rawSearch") String rawSearch,
            @Param("likeSearch") String likeSearch,
            @Param("mealTypes") List<MealType> mealTypes,
            @Param("cookTimes") List<CookTime> cookTimes,
            @Param("cookComplexities") List<CookComplexity> cookComplexities,
            @Param("cookBudgets") List<CookBudget> cookBudgets,
            @Param("tags") List<String> tags,
            Pageable pageable
    );

    @Query("SELECT r FROM RecipeEntity r LEFT JOIN FETCH r.nutrition")
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

    @Query("""
        SELECT DISTINCT r FROM RecipeEntity r
        JOIN r.ingredients ri
        WHERE ri.ingredient.id IN :ingredientIds
    """)
    Page<RecipeEntity> findAllByIngredientsIds(@Param("ingredientIds") List<Long> ingredientIds, Pageable pageable);

        @Query("""
        SELECT
            r.id as recipeId,
            r.name as recipeName,
            r.imageUrl as imageUrl,
            SUM(CASE WHEN ri.ingredient.id IN :ingredientIds THEN 1 ELSE 0 END) as matchedCount,
            COUNT(ri) as totalIngredients,
            (
                SUM(CASE WHEN ri.ingredient.id IN :ingredientIds THEN 1 ELSE 0 END) * 1.0
                / COUNT(ri)
            ) as matchPercent
        FROM RecipeEntity r
        JOIN r.ingredients ri
        GROUP BY r.id, r.name, r.imageUrl
        HAVING SUM(CASE WHEN ri.ingredient.id IN :ingredientIds THEN 1 ELSE 0 END) > 0
        ORDER BY
            (
                SUM(CASE WHEN ri.ingredient.id IN :ingredientIds THEN 1 ELSE 0 END) * 1.0
                / COUNT(ri)
            ) DESC,
            SUM(CASE WHEN ri.ingredient.id IN :ingredientIds THEN 1 ELSE 0 END) DESC
    """)
        Page<RecipeMatchProjection> findRecipesRankedByIngredientMatch(
                @Param("ingredientIds") List<Long> ingredientIds,
                Pageable pageable
        );
}
