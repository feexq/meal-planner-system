package com.feex.mealplannersystem.dto.order;

import com.feex.mealplannersystem.common.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long id;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String trackingNumber;
    private LocalDateTime createdAt;
}