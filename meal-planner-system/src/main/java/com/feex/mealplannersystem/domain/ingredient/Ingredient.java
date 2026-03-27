package com.feex.mealplannersystem.domain.ingredient;

import com.feex.mealplannersystem.common.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {
    private Long id;
    private String normalizedName;
    private String slug;
    private String imageUrl;
    private BigDecimal price;
    private Unit unit;
    private Integer stock;
    private boolean available;
    private Long categoryId;
    private String categoryName;
    private List<String> aliases;
}
