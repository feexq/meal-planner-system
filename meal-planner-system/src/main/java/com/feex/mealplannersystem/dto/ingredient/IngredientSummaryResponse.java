package com.feex.mealplannersystem.dto.ingredient;

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
public class IngredientSummaryResponse {
    private Long id;
    private String normalizedName;
    private String slug;
    private String imageUrl;
    private BigDecimal price;
    private Unit unit;
    private boolean available;
    private Long categoryId;
    private String categoryName;
}
