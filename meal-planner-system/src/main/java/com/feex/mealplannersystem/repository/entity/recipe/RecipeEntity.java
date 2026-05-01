package com.feex.mealplannersystem.repository.entity.recipe;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RecipeEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "ingredients_raw_str", columnDefinition = "TEXT")
    private String ingredientsRawStr;

    @Column(name = "serving_size")
    private String servingSize;

    private Integer servings;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type")
    private MealType mealType;

    @Column(name = "meal_type_detailed")
    private String mealTypeDetailed;

    @Enumerated(EnumType.STRING)
    @Column(name = "cook_time")
    private CookTime cookTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "cook_complexity")
    private CookComplexity cookComplexity;

    @Enumerated(EnumType.STRING)
    @Column(name = "cook_budget")
    private CookBudget cookBudget;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private Set<RecipeStepEntity> steps = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 100)
    private Set<RecipeIngredientEntity> ingredients = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "recipe_tags",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    @BatchSize(size = 100)
    private Set<RecipeTagEntity> tags = new HashSet<>();

    @OneToOne(mappedBy = "recipe", cascade = CascadeType.ALL)
    private RecipeNutritionEntity nutrition;
}
