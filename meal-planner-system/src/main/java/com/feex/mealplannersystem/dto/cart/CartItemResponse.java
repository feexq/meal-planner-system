package com.feex.mealplannersystem.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long ingredientId;
    private String normalizedName;
    private String slug;
    private String imageUrl;
    private BigDecimal price;
    private String unit;
    private Integer quantity;
    private BigDecimal totalPrice;
}
