package com.feex.mealplannersystem.repository.entity.ingredient;

import com.feex.mealplannersystem.common.mealplan.DietaryTagStatus;
import com.feex.mealplannersystem.repository.entity.DietaryConditionEntity;
import com.feex.mealplannersystem.repository.other.IngredientDietaryTagId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredient_dietary_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientDietaryTagEntity {

    @EmbeddedId
    private IngredientDietaryTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;


    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conditionId")
    @JoinColumn(name = "condition_id")
    private DietaryConditionEntity condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DietaryTagStatus status;

    @Override
    public String toString() {
        return ingredient.getNormalizedName() + condition + status;
    }
}
