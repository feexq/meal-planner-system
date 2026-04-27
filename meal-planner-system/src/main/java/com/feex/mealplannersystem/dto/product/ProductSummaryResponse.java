package com.feex.mealplannersystem.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private Long id;
    private String nameUk;
    private String slug;
    private String imageUrl;
    private BigDecimal price;
    private String unit;
    private boolean isAvailable;
    private Long categoryId;
    private String categoryName;

}