package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.dto.recipe.RecipeMatchResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeSummaryResponse;
import com.feex.mealplannersystem.service.RecipeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "Recipes")
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public ResponseEntity<Page<RecipeSummaryResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) CookTime cookTime,
            @RequestParam(required = false) CookComplexity cookComplexity,
            @RequestParam(required = false) CookBudget cookBudget,
            @RequestParam(required = false) String tag,
            @ParameterObject
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(
                recipeService.getAll(search, mealType, cookTime, cookComplexity, cookBudget, tag, pageable)
        );
    }

    @GetMapping("/filters")
    public ResponseEntity<Page<RecipeSummaryResponse>> getMarketplaceRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "mealTypes") List<MealType> mealTypes,
            @RequestParam(required = false, name = "cookTimes") List<CookTime> cookTimes,
            @RequestParam(required = false, name = "cookComplexities") List<CookComplexity> cookComplexities,
            @RequestParam(required = false, name = "cookBudgets") List<CookBudget> cookBudgets,
            @RequestParam(required = false, name = "tags") List<String> tags,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(
                recipeService.getMarketplaceRecipes(search, mealTypes, cookTimes, cookComplexities, cookBudgets, tags, pageable)
        );
    }

    @GetMapping("/details")
    public ResponseEntity<Page<RecipeResponse>> getAllWithFilters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) CookTime cookTime,
            @RequestParam(required = false) CookComplexity cookComplexity,
            @RequestParam(required = false) CookBudget cookBudget,
            @RequestParam(required = false) String tag,
            @ParameterObject
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ){
        return ResponseEntity.ok(
                recipeService.getAllWithDetails(search, mealType, cookTime, cookComplexity, cookBudget, tag, pageable)
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<RecipeResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(recipeService.getBySlug(slug));
    }

    @GetMapping("/by-ingredient/{ingredientId}")
    public ResponseEntity<Page<RecipeSummaryResponse>> getByIngredient(
            @PathVariable Long ingredientId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(recipeService.getByIngredient(ingredientId, pageable));
    }

    @GetMapping("/search/by-ingredients")
    public ResponseEntity<List<RecipeMatchResponse>> searchByIngredients(
            @RequestParam List<Long> ingredientIds
    ) {
        return ResponseEntity.ok(
                recipeService.findRecipesByIngredients(ingredientIds)
        );
    }
}
