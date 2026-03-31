package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.order.CheckoutRequest;
import com.feex.mealplannersystem.dto.order.CheckoutResponse;

public interface CheckoutService {
    CheckoutResponse createOrderAndPaymentSession(String cartKey, CheckoutRequest request);
}