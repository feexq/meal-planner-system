package com.feex.mealplannersystem.dto.recipe;

import com.feex.mealplannersystem.common.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientResponse {
    private Long ingredientId;
    private String rawName;
    private String rawAmount;
    private boolean availableInShop;
    private BigDecimal price;
    private Unit unit;
    private String imageUrl;
}
