package com.feex.mealplannersystem.repository.entity.ingredient;

import com.feex.mealplannersystem.common.Unit;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class IngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private Unit unit;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "is_available", nullable = false)
    private boolean available = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientAliasEntity> aliases = new ArrayList<>();
}
