package com.feex.mealplannersystem.config.normalizer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IngredientNormalizationQueue {

    private static final String QUEUE_KEY = "normalization:queue";

    private final StringRedisTemplate redisTemplate;

    public void add(Long ingredientId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, ingredientId.toString());
    }

    public List<Long> popBatch(int size) {
        List<Long> batch = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String val = redisTemplate.opsForList().leftPop(QUEUE_KEY);
            if (val == null) break;
            batch.add(Long.parseLong(val));
        }
        return batch;
    }

    public long size() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0;
    }
}
