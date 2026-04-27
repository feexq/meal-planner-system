package com.feex.mealplannersystem.dto.ingredient;

import com.feex.mealplannersystem.common.product.Unit;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIngredientRequest {

    @NotBlank
    private String normalizedName;
    private String imageUrl;
    private Boolean available;
    private BigDecimal price;
    private Unit unit;
    private Integer stock;
    private List<String> aliases;
    private Long categoryId;
}
