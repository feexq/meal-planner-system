package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.order.OrderDetailsResponse;
import com.feex.mealplannersystem.dto.order.OrderSummaryResponse;

import java.util.List;

public interface OrderService {
    void processSuccessfulPayment(String stripeSessionId);
    List<OrderSummaryResponse> getUserOrderHistory(Long userId);
    OrderDetailsResponse getOrderDetails(Long orderId, Long userId);
}