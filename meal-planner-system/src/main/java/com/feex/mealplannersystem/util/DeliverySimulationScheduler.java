package com.feex.mealplannersystem.util;

import com.feex.mealplannersystem.common.OrderStatus;
import com.feex.mealplannersystem.repository.OrderRepository;
import com.feex.mealplannersystem.repository.entity.order.OrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverySimulationScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void simulateDeliveryProgress() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime twoMinutesAgo = now.minusMinutes(2);
        List<OrderEntity> paidOrders = orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.PAID, twoMinutesAgo);

        for (OrderEntity order : paidOrders) {
            order.setStatus(OrderStatus.IN_TRANSIT);
            log.info("🚚 Замовлення ID {} відправлено (IN_TRANSIT)", order.getId());
        }
        orderRepository.saveAll(paidOrders);

        LocalDateTime threeMinutesAgo = now.minusMinutes(3);
        List<OrderEntity> inTransitOrders = orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.IN_TRANSIT, threeMinutesAgo);

        for (OrderEntity order : inTransitOrders) {
            order.setStatus(OrderStatus.DELIVERED);
            log.info("📦 Замовлення ID {} успішно доставлено (DELIVERED)", order.getId());
        }
        orderRepository.saveAll(inTransitOrders);
    }
}