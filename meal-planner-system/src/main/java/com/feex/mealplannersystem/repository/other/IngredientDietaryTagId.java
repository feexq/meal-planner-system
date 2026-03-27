package com.feex.mealplannersystem.repository.other;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDietaryTagId implements Serializable {
    private Long ingredientId;
    private String conditionId;
}