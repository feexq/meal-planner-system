package com.feex.mealplannersystem.repository.entity.product;

import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_uk", nullable = false)
    private String nameUk;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "unit")
    private String unit;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "is_available")
    private boolean available;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "protein_g")
    private Double proteinG;

    @Column(name = "fat_g")
    private Double fatG;

    @Column(name = "carbs_g")
    private Double carbsG;

    @Column(name = "calorie_confidence")
    private Double calorieConfidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<BaseTagEntity> tags = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "package_amount")
    private Double packageAmount;

    @Column(name = "package_unit")
    private String packageUnit;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}