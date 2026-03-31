package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.order.CheckoutRequest;
import com.feex.mealplannersystem.dto.order.CheckoutResponse;
import com.feex.mealplannersystem.dto.order.OrderDetailsResponse;
import com.feex.mealplannersystem.dto.order.OrderSummaryResponse;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.CheckoutService;
import com.feex.mealplannersystem.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(@RequestBody @Valid CheckoutRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = (UserEntity) auth.getPrincipal();

        String cartKey = "cart:user:" + user.getId();
        CheckoutResponse response = checkoutService.createOrderAndPaymentSession(cartKey, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = (UserEntity) auth.getPrincipal();

        return ResponseEntity.ok(orderService.getUserOrderHistory(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = (UserEntity) auth.getPrincipal();

        return ResponseEntity.ok(orderService.getOrderDetails(id, user.getId()));
    }
}
