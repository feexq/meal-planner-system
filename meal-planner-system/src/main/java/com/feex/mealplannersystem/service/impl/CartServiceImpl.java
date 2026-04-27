package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.cart.AddToCartRequest;
import com.feex.mealplannersystem.dto.cart.CartItemResponse;
import com.feex.mealplannersystem.dto.cart.CartResponse;
import com.feex.mealplannersystem.dto.cart.UpdateCartItemRequest;
import com.feex.mealplannersystem.dto.recipe.RecipeIngredientDetail;
import com.feex.mealplannersystem.repository.ProductRepository;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.RecipeTranslationRepository;
import com.feex.mealplannersystem.repository.entity.product.ProductEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeTranslationEntity;
import com.feex.mealplannersystem.service.CartService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.exception.IngredientNotAvailableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeTranslationRepository translationRepository;

    private static final int USER_CART_TTL_DAYS = 30;
    private static final int SESSION_CART_TTL_DAYS = 7;

    @Override
    public CartResponse getCart(String cartKey) {
        Map<Object, Object> cartData = redisTemplate.opsForHash().entries(cartKey);
        return buildCartResponse(cartData);
    }

    @Override
    public CartResponse addItem(String cartKey, AddToCartRequest request) {
        ProductEntity product = productRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new CustomNotFoundException("Product", request.getIngredientId().toString()));

        if (!product.isAvailable()) {
            throw new IngredientNotAvailableException(product.getNameUk());
        }

        String field = request.getIngredientId().toString();

        Object existing = redisTemplate.opsForHash().get(cartKey, field);
        int currentQty = existing != null ? Integer.parseInt(existing.toString()) : 0;
        int newQty = currentQty + request.getQuantity();

        redisTemplate.opsForHash().put(cartKey, field, String.valueOf(newQty));
        setTtl(cartKey);

        return getCart(cartKey);
    }

    @Override
    public CartResponse updateItem(String cartKey, Long productId, UpdateCartItemRequest request) {
        String field = productId.toString();

        if (!redisTemplate.opsForHash().hasKey(cartKey, field)) {
            throw new CustomNotFoundException("Ingredient", productId.toString());
        }

        redisTemplate.opsForHash().put(cartKey, field, String.valueOf(request.getQuantity()));
        setTtl(cartKey);

        return getCart(cartKey);
    }

    @Override
    public CartResponse removeItem(String cartKey, Long productId) {
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
        return getCart(cartKey);
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void mergeCarts(String sessionKey, String userKey) {
        Map<Object, Object> sessionCart = redisTemplate.opsForHash().entries(sessionKey);
        if (sessionCart.isEmpty()) return;

        sessionCart.forEach((ingredientId, quantity) -> {
            Object existing = redisTemplate.opsForHash().get(userKey, ingredientId);
            int sessionQty = Integer.parseInt(quantity.toString());
            int currentQty = existing != null ? Integer.parseInt(existing.toString()) : 0;
            int finalQty = sessionQty + currentQty;
            redisTemplate.opsForHash().put(userKey, ingredientId.toString(), String.valueOf(finalQty));
        });

        redisTemplate.delete(sessionKey);
        setTtl(userKey);
    }

    @Override
    public CartResponse addRecipeIngredients(Long recipeId, String cartKey) {
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Recipe not found: " + recipeId));

        Optional<RecipeTranslationEntity> ukTranslation = translationRepository
                .findByRecipeIdAndLanguageCode(recipeId, "uk");

        List<RecipeIngredientDetail> parsedIngredients = ukTranslation
                .map(RecipeTranslationEntity::getIngredients)
                .orElse(List.of());

        recipe.getIngredients().stream()
                .filter(ri -> ri.getIngredient().getProduct() != null
                        && ri.getIngredient().getProduct().isAvailable())
                .forEach(ri -> {
                    ProductEntity product = ri.getIngredient().getProduct();

                    // 2. Рахуємо кількість за допомогою нашої функції
                    int qtyToAdd = calculateRequiredPackages(ri, product, parsedIngredients);

                    String field = product.getId().toString();
                    Object existing = redisTemplate.opsForHash().get(cartKey, field);
                    int currentQty = existing != null ? Integer.parseInt(existing.toString()) : 0;

                    redisTemplate.opsForHash().put(cartKey, field, String.valueOf(currentQty + qtyToAdd));
                });

        setTtl(cartKey);
        return getCart(cartKey);
    }

    private CartResponse buildCartResponse(Map<Object, Object> cartData) {
        if (cartData.isEmpty()) {
            return CartResponse.builder()
                    .items(new ArrayList<>())
                    .totalPrice(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();
        }

        List<Long> ingredientIds = cartData.keySet().stream()
                .map(k -> Long.parseLong(k.toString()))
                .toList();

        Map<Long, ProductEntity> productsMap = productRepository.findAllById(ingredientIds)
                .stream()
                .collect(Collectors.toMap(ProductEntity::getId, i -> i));

        List<CartItemResponse> items = cartData.entrySet().stream()
                .map(entry -> {
                    Long ingredientId = Long.parseLong(entry.getKey().toString());
                    int quantity = Integer.parseInt(entry.getValue().toString());
                    ProductEntity ingredient = productsMap.get(ingredientId); // Змінна названа ingredient, але це ProductEntity (залишаю як в тебе)

                    if (ingredient == null) {
                        return null;
                    }

                    BigDecimal itemTotal = ingredient.getPrice() != null
                            ? ingredient.getPrice().multiply(BigDecimal.valueOf(quantity))
                            : BigDecimal.ZERO;

                    return CartItemResponse.builder()
                            .ingredientId(ingredientId)
                            .normalizedName(ingredient.getNameUk())
                            .slug(ingredient.getSlug())
                            .imageUrl(ingredient.getImageUrl())
                            .price(ingredient.getPrice())
                            .unit(ingredient.getUnit()) // Повертає старий текстовий unit для UI
                            .quantity(quantity)
                            .totalPrice(itemTotal)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(items)
                .totalPrice(totalPrice)
                .totalItems(items.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .build();
    }

    private void setTtl(String cartKey) {
        long days = cartKey.startsWith("cart:user:") ? USER_CART_TTL_DAYS : SESSION_CART_TTL_DAYS;
        redisTemplate.expire(cartKey, days, TimeUnit.DAYS);
    }

    private int calculateRequiredPackages(
            RecipeIngredientEntity dbEntity,
            ProductEntity product,
            List<RecipeIngredientDetail> parsedList) {

        RecipeIngredientDetail detail = parsedList.stream()
                .filter(p -> {
                    if (p.getNameUk() == null || product.getNameUk() == null) return false;
                    String jsonName = p.getNameUk().toLowerCase();
                    String productName = product.getNameUk().toLowerCase();
                    return productName.contains(jsonName) || jsonName.contains(productName);
                })
                .findFirst()
                .orElse(null);

        if (detail == null || detail.getAmount() == null || detail.getUnit() == null) {
            return 1;
        }

        double recipeAmount = detail.getAmount();
        String recipeUnit = detail.getUnit().toLowerCase();

        double packageAmount = product.getPackageAmount() != null ? product.getPackageAmount() : 1.0;
        String packageUnit = product.getPackageUnit() != null ? product.getPackageUnit().toLowerCase() : recipeUnit;

        double convertedRecipeAmount = convertToMatchShopUnit(recipeAmount, recipeUnit, packageUnit);

        if (packageAmount > 0) {
            return (int) Math.ceil(convertedRecipeAmount / packageAmount);
        }

        return 1;
    }

    private double convertToMatchShopUnit(double amount, String fromUnit, String toUnit) {
        if (fromUnit == null || toUnit == null) return amount;

        fromUnit = fromUnit.trim().toLowerCase();
        toUnit = toUnit.trim().toLowerCase();

        if (fromUnit.equals(toUnit)) return amount;

        boolean toIsKg = toUnit.equals("кг");
        boolean toIsG = toUnit.equals("г");
        boolean toIsL = toUnit.equals("л");
        boolean toIsMl = toUnit.equals("мл") || toUnit.equals("ml");

        if (fromUnit.equals("ст.л.") || fromUnit.equals("ст. л.")) {
            if (toIsG || toIsMl) return amount * 15.0;
            if (toIsKg || toIsL) return (amount * 15.0) / 1000.0;
        }
        if (fromUnit.equals("ч.л.") || fromUnit.equals("ч. л.")) {
            if (toIsG || toIsMl) return amount * 5.0;
            if (toIsKg || toIsL) return (amount * 5.0) / 1000.0;
        }

        if (fromUnit.equals("склянка") || fromUnit.equals("склянки")) {
            if (toIsG || toIsMl) return amount * 250.0;
            if (toIsKg || toIsL) return (amount * 250.0) / 1000.0;
        }

        if (fromUnit.equals("кг") && toIsG) return amount * 1000.0;
        if (fromUnit.equals("г") && toIsKg) return amount / 1000.0;

        if (fromUnit.equals("л") && toIsMl) return amount * 1000.0;
        if ((fromUnit.equals("мл") || fromUnit.equals("ml")) && toIsL) return amount / 1000.0;

        if ((fromUnit.equals("мл") || fromUnit.equals("ml")) && toIsG) return amount;
        if ((fromUnit.equals("мл") || fromUnit.equals("ml")) && toIsKg) return amount / 1000.0;
        if (fromUnit.equals("л") && toIsG) return amount * 1000.0;
        if (fromUnit.equals("л") && toIsKg) return amount;
        if (fromUnit.equals("г") && toIsMl) return amount;
        if (fromUnit.equals("г") && toIsL) return amount / 1000.0;
        if (fromUnit.equals("кг") && toIsMl) return amount * 1000.0;
        if (fromUnit.equals("кг") && toIsL) return amount;

        if (fromUnit.equals("шт") || fromUnit.equals("штук")) {
            if (toIsG) return amount * 150.0;
            if (toIsKg) return (amount * 150.0) / 1000.0;
        }

        if (fromUnit.contains("смак") || fromUnit.contains("дрібк")) {
            return 0.01;
        }

        return amount;
    }
}