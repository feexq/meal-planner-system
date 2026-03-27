package com.feex.mealplannersystem.repository.entity.ingredient;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ingredient_aliases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class IngredientAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_name", nullable = false, unique = true)
    private String rawName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private IngredientEntity ingredient;
}