package com.feex.mealplannersystem.repository.entity.recipe;

import com.feex.mealplannersystem.dto.recipe.RecipeIngredientDetail;
import com.feex.mealplannersystem.dto.recipe.RecipeStepDetail;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "recipe_translations")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RecipeTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeEntity recipe;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode; // 'uk', 'en', 'pl' тощо

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ingredients_json")
    private List<RecipeIngredientDetail> ingredients;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json")
    private List<RecipeStepDetail> steps;

    @Column(name = "serving_size")
    private String servingSize;
}