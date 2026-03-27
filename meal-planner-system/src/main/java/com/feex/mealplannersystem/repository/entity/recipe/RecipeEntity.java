package com.feex.mealplannersystem.repository.entity.recipe;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
@Data
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
    private List<RecipeStepEntity> steps = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "recipe_tags",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<TagEntity> tags = new ArrayList<>();

    @OneToOne(mappedBy = "recipe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RecipeNutritionEntity nutrition;
}
