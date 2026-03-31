package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.OrderStatus;
import com.feex.mealplannersystem.dto.cart.CartItemResponse;
import com.feex.mealplannersystem.dto.cart.CartResponse;
import com.feex.mealplannersystem.dto.order.CheckoutRequest;
import com.feex.mealplannersystem.dto.order.CheckoutResponse;
import com.feex.mealplannersystem.repository.OrderRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.entity.order.OrderEntity;
import com.feex.mealplannersystem.repository.entity.order.OrderItemEntity;
import com.feex.mealplannersystem.service.CartService;
import com.feex.mealplannersystem.service.CheckoutService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final OrderRepository orderRepository;

    @Value("${spring.stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    @Transactional
    public CheckoutResponse createOrderAndPaymentSession(String cartKey, CheckoutRequest request) {
        CartResponse cart = cartService.getCart(cartKey);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Кошик порожній! Неможливо оформити замовлення.");
        }

        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        OrderEntity order = OrderEntity.builder()
                .user(currentUser)
                .totalAmount(cart.getTotalPrice())
                .status(OrderStatus.PENDING)
                .npCityRef(request.getNpCityRef())
                .npWarehouseRef(request.getNpWarehouseRef())
                .build();

        for (CartItemResponse item : cart.getItems()) {
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .ingredient(IngredientEntity.builder().id(item.getIngredientId()).build())
                    .quantity(item.getQuantity())
                    .priceAtPurchase(item.getPrice())
                    .build();
            order.addItem(orderItem);
        }

        try {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/checkout/cancel");

            // Додаємо кожен товар зі списку у чек Stripe
            for (CartItemResponse item : cart.getItems()) {
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) item.getQuantity())
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("uah")
                                                .setUnitAmount(item.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(item.getNormalizedName())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );
            }

            Session session = Session.create(paramsBuilder.build());

            order.setStripeSessionId(session.getId());
            orderRepository.save(order);

            return new CheckoutResponse(session.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException("Помилка при створенні платіжної сесії Stripe: " + e.getMessage());
        }
    }
}