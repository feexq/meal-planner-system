package com.feex.mealplannersystem.dto.ingredient;

import com.feex.mealplannersystem.common.Unit;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIngredientRequest {

    @NotBlank
    private String normalizedName;

    private String imageUrl;

    private boolean available = false;

    private BigDecimal price;

    private Unit unit;

    private Integer stock;

    private List<String> aliases = new ArrayList<>();

    private Long categoryId;
}
