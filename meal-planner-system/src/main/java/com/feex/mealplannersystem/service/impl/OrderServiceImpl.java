package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.OrderStatus;
import com.feex.mealplannersystem.dto.order.OrderDetailsResponse;
import com.feex.mealplannersystem.dto.order.OrderItemDto;
import com.feex.mealplannersystem.dto.order.OrderSummaryResponse;
import com.feex.mealplannersystem.repository.OrderRepository;
import com.feex.mealplannersystem.repository.entity.order.OrderEntity;
import com.feex.mealplannersystem.service.CartService;
import com.feex.mealplannersystem.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Override
    @Transactional
    public void processSuccessfulPayment(String stripeSessionId) {
        log.info("Обробка успішного платежу для сесії: {}", stripeSessionId);

        OrderEntity order = orderRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення з сесією " + stripeSessionId + " не знайдено"));

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Замовлення ID {} вже має статус PAID", order.getId());
            return;
        }

        order.setStatus(OrderStatus.PAID);

        String mockTtn = generateMockNovaPoshtaTtn();
        order.setTrackingNumber(mockTtn);

        orderRepository.save(order);
        log.info("Замовлення ID {} успішно оновлено. Згенеровано ТТН: {}", order.getId(), mockTtn);

        String userCartKey = "cart:user:" + order.getUser().getId();
        cartService.clearCart(userCartKey);
        log.info("Кошик {} очищено", userCartKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getUserOrderHistory(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> OrderSummaryResponse.builder()
                        .id(order.getId())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .trackingNumber(order.getTrackingNumber())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetails(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Замовлення не знайдено або доступ заборонено"));

        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .ingredientId(item.getIngredient().getId())
                        .name(item.getIngredient().getNormalizedName())
                        .imageUrl(item.getIngredient().getImageUrl())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .build())
                .toList();

        return OrderDetailsResponse.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .npCityRef(order.getNpCityRef())
                .npWarehouseRef(order.getNpWarehouseRef())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private String generateMockNovaPoshtaTtn() {
        Random random = new Random();
        long randomPart = 1000000000L + (long)(random.nextDouble() * 8999999999L);
        return "2045" + randomPart;
    }
}