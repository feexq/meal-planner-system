package com.feex.mealplannersystem.repository.entity.recipe;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recipe_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private RecipeEntity recipe;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
}
