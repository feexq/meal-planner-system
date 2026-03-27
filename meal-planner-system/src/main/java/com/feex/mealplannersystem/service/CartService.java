package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.cart.AddToCartRequest;
import com.feex.mealplannersystem.dto.cart.CartResponse;
import com.feex.mealplannersystem.dto.cart.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCart(String cartKey);
    CartResponse addItem(String cartKey, AddToCartRequest request);
    CartResponse updateItem(String cartKey, Long ingredientId, UpdateCartItemRequest request);
    CartResponse removeItem(String cartKey, Long ingredientId);
    CartResponse addRecipeIngredients(Long recipeId, String cartKey);
    void clearCart(String cartKey);
    void mergeCarts(String sessionKey, String userKey);
}
