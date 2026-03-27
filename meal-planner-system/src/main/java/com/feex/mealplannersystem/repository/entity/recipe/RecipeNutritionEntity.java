package com.feex.mealplannersystem.repository.entity.recipe;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_nutrition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeNutritionEntity {

    @Id
    private Long recipeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe;

    @Column(name = "serving_size")
    private String servingSize;

    @Column(name = "servings_per_recipe")
    private Integer servingsPerRecipe;

    @Column(name = "calories")
    private BigDecimal calories;

    @Column(name = "calories_from_fat")
    private BigDecimal caloriesFromFat;

    @Column(name = "total_fat_g")
    private BigDecimal totalFatG;

    @Column(name = "saturated_fat_g")
    private BigDecimal saturatedFatG;

    @Column(name = "cholesterol_mg")
    private BigDecimal cholesterolMg;

    @Column(name = "sodium_mg")
    private BigDecimal sodiumMg;

    @Column(name = "total_carbs_g")
    private BigDecimal totalCarbsG;

    @Column(name = "dietary_fiber_g")
    private BigDecimal dietaryFiberG;

    @Column(name = "sugars_g")
    private BigDecimal sugarsG;

    @Column(name = "protein_g")
    private BigDecimal proteinG;
}