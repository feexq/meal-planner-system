package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class FoodNutritionCacheServiceImpl {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KEY_PREFIX = "food_nutrition:";
    private static final Duration TTL       = Duration.ofDays(30);

    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class CachedNutrition {
        private double calories;
        private double proteinG;
        private double carbsG;
        private double fatG;
        private String quantityDescription;
    }

    public static String normalizeKey(String foodName) {
        return KEY_PREFIX + foodName.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }

    public Optional<CachedNutrition> get(String foodName) {
        String key = normalizeKey(foodName);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(value, CachedNutrition.class));
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String foodName, CachedNutrition nutrition) {
        String key = normalizeKey(foodName);
        try {
            String value = objectMapper.writeValueAsString(nutrition);
            redisTemplate.opsForValue().set(key, value, TTL);
            log.debug("Cached nutrition for: {}", foodName);
        } catch (JsonProcessingException e) {
            log.warn("Redis write failed for key {}: {}", key, e.getMessage());
        }
    }

    public void evict(String foodName) {
        redisTemplate.delete(normalizeKey(foodName));
    }
}

