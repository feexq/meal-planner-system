package com.feex.mealplannersystem.dto.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeIngredientDto {
    @NotNull
    Long ingredientId;
    
    @NotBlank
    String rawName;
    
    @NotBlank
    String rawAmount;
}
