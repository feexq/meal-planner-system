package com.feex.mealplannersystem.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String nameUk;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private String imageUrl;

    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private Boolean isAvailable;

    @Min(value = 0, message = "Calories cannot be negative")
    private Double calories;

    @Min(value = 0, message = "Protein cannot be negative")
    private Double proteinG;

    @Min(value = 0, message = "Fat cannot be negative")
    private Double fatG;

    @Min(value = 0, message = "Carbohydrates cannot be negative")
    private Double carbsG;

    private Double calorieConfidence;

    private Long categoryId;

    private Set<Long> tagIds;
}