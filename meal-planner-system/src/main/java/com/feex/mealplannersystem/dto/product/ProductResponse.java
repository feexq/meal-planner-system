package com.feex.mealplannersystem.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String nameUk;
    private String slug;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String unit;
    private Integer stock;
    private boolean isAvailable;
    private Double calories;
    private Double proteinG;
    private Double fatG;
    private Double carbsG;
    private Double calorieConfidence;
    private Long categoryId;
    private String categoryName;
    private Set<String> tags;
    private LocalDateTime createdAt;
}