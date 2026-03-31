package com.feex.mealplannersystem.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long ingredientId;
    private String name;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
}