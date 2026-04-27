package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.common.order.OrderStatus;
import com.feex.mealplannersystem.repository.entity.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByStripeSessionId(String stripeSessionId);

    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    List<OrderEntity> findByStatusAndUpdatedAtBefore(OrderStatus status, LocalDateTime time);
}
