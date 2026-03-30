package com.feex.mealplannersystem.repository.entity.recipe;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Exclude // <--- ДОДАТИ ЦЕ
    @ToString.Exclude          // <--- ДОДАТИ ЦЕ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeEntity recipe;

    @EqualsAndHashCode.Exclude // <--- ДОДАТИ ЦЕ
    @ToString.Exclude          // <--- ДОДАТИ ЦЕ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Column(name = "raw_name", nullable = false)
    private String rawName;
}
