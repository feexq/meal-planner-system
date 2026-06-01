package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feex.mealplannersystem.service.DietaryNotesCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietaryNotesCacheServiceImpl implements DietaryNotesCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KEY_PREFIX = "dietary_notes:";
    private static final Duration TTL = Duration.ofDays(10); // Трохи більше тижня, щоб точно покрити активний план

    private String buildKey(Long userId, Long recipeId) {
        return KEY_PREFIX + userId + ":" + recipeId;
    }

    @Override
    public void putNotes(Long userId, Long recipeId, List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        
        String key = buildKey(userId, recipeId);
        try {
            String value = objectMapper.writeValueAsString(notes);
            redisTemplate.opsForValue().set(key, value, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to save dietary notes to Redis for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public List<String> getNotes(Long userId, Long recipeId) {
        String key = buildKey(userId, recipeId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to read dietary notes from Redis for key {}: {}", key, e.getMessage());
            return null;
        }
    }
}
