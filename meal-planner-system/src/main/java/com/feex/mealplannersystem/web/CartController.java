package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.cart.AddToCartRequest;
import com.feex.mealplannersystem.dto.cart.CartResponse;
import com.feex.mealplannersystem.dto.cart.UpdateCartItemRequest;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.CartService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getCart(resolveCartKey(request)));
    }

    @PostMapping("/items")
    @Operation(parameters = {
            @Parameter(ref = "#/components/parameters/cartSessionHeader")
    })
    public ResponseEntity<CartResponse> addItem(
            HttpServletRequest request,
            @RequestBody @Valid AddToCartRequest body
    ) {
        return ResponseEntity.ok(cartService.addItem(resolveCartKey(request), body));
    }

    @PutMapping("/items/{ingredientId}")
    public ResponseEntity<CartResponse> updateItem(
            HttpServletRequest request,
            @PathVariable Long ingredientId,
            @RequestBody @Valid UpdateCartItemRequest body
    ) {
        return ResponseEntity.ok(cartService.updateItem(resolveCartKey(request), ingredientId, body));
    }

    @DeleteMapping("/items/{ingredientId}")
    public ResponseEntity<CartResponse> removeItem(
            HttpServletRequest request,
            @PathVariable Long ingredientId
    ) {
        return ResponseEntity.ok(cartService.removeItem(resolveCartKey(request), ingredientId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        cartService.clearCart(resolveCartKey(request));
        return ResponseEntity.noContent().build();
    }

    private String resolveCartKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            UserEntity user = (UserEntity) auth.getPrincipal();
            return "cart:user:" + user.getId();
        }

        String sessionId = request.getHeader("X-Cart-Session");
        if (sessionId != null && !sessionId.isBlank()) {
            return "cart:session:" + sessionId;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Cart-Session header is required for guests");
    }

    @PostMapping("/add-recipe/{recipeId}")
    public ResponseEntity<CartResponse> addRecipeIngredients(
            @PathVariable Long recipeId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                cartService.addRecipeIngredients(recipeId, resolveCartKey(request))
        );
    }
}
