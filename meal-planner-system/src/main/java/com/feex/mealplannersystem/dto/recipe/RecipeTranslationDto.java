package com.feex.mealplannersystem.dto.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTranslationDto {
    @NotBlank
    String name;

    String description;

    @NotNull
    @NotEmpty
    List<@Valid RecipeStepDetail> steps;

    @NotNull
    @NotEmpty
    List<@Valid RecipeIngredientDetail> ingredients;

    String servingSize;
}
