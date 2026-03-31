package com.feex.mealplannersystem.dto.order;

import com.feex.mealplannersystem.common.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsResponse {
    private Long id;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String npCityRef;
    private String npWarehouseRef;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private List<OrderItemDto> items;
}