package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.cart.AddToCartRequest;
import com.feex.mealplannersystem.dto.cart.CartResponse;
import com.feex.mealplannersystem.dto.cart.UpdateCartItemRequest;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.CartService;
import com.feex.mealplannersystem.util.ResolveCartKeyHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;
    private final ResolveCartKeyHelper resolveCartKeyHelper;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getCart(resolveCartKeyHelper.resolveCartKey(request)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            HttpServletRequest request,
            @RequestBody @Valid AddToCartRequest body
    ) {
        return ResponseEntity.ok(cartService.addItem(resolveCartKeyHelper.resolveCartKey(request), body));
    }

    @PutMapping("/items/{ingredientId}")
    public ResponseEntity<CartResponse> updateItem(
            HttpServletRequest request,
            @PathVariable Long ingredientId,
            @RequestBody @Valid UpdateCartItemRequest body
    ) {
        return ResponseEntity.ok(cartService.updateItem(resolveCartKeyHelper.resolveCartKey(request), ingredientId, body));
    }

    @DeleteMapping("/items/{ingredientId}")
    public ResponseEntity<CartResponse> removeItem(
            HttpServletRequest request,
            @PathVariable Long ingredientId
    ) {
        return ResponseEntity.ok(cartService.removeItem(resolveCartKeyHelper.resolveCartKey(request), ingredientId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        cartService.clearCart(resolveCartKeyHelper.resolveCartKey(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add-recipe/{recipeId}")
    public ResponseEntity<CartResponse> addRecipeIngredients(
            @PathVariable Long recipeId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                cartService.addRecipeIngredients(recipeId, resolveCartKeyHelper.resolveCartKey(request))
        );
    }

    @PostMapping("/merge")
    public ResponseEntity<Void> mergeCart(
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionId,
            @AuthenticationPrincipal UserEntity user) {
        if (sessionId != null && !sessionId.isBlank() && user != null) {
            cartService.mergeCarts("cart:session:" + sessionId, "cart:user:" + user.getId());
        }
        return ResponseEntity.ok().build();
    }


}
