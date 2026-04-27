package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feex.mealplannersystem.dto.delivery.DeliveryLocationDto;
import com.feex.mealplannersystem.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.novaposhta.api.key}")
    private String apiKey;

    @Value("${spring.novaposhta.api.url}")
    private String apiUrl;

    @Override
    public List<DeliveryLocationDto> searchCities(String cityName) {
        String cacheKey = "np:cities:" + (cityName != null ? cityName.toLowerCase().trim() : "all");

        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("Cities retrieved from Redis cache for: {}", cityName);
                return objectMapper.readValue(cachedData, new TypeReference<List<DeliveryLocationDto>>() {});
            }

            log.info("Cache is empty. Making request to Nova Poshta API for cities: {}", cityName);

            Map<String, Object> methodProperties = new HashMap<>();
            if (cityName != null && !cityName.isBlank()) {
                methodProperties.put("FindByString", cityName);
            }
            methodProperties.put("Limit", "50");

            List<DeliveryLocationDto> cities = makeApiCall("Address", "getCities", methodProperties);

            if (!cities.isEmpty()) {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(cities),
                        Duration.ofHours(24)
                );
            }

            return cities;

        } catch (Exception e) {
            log.error("Error searching for cities: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<DeliveryLocationDto> getWarehouses(String cityRef, String search) {
        if (search != null && !search.isBlank()) {
            try {
                Map<String, Object> methodProperties = new HashMap<>();
                methodProperties.put("CityRef", cityRef);
                methodProperties.put("FindByString", search);
                return makeApiCall("Address", "getWarehouses", methodProperties);
            } catch (Exception e) {
                log.error("Error searching for warehouses: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        }

        String cacheKey = "np:warehouses:" + cityRef;
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, new TypeReference<>() {});
            }

            Map<String, Object> methodProperties = new HashMap<>();
            methodProperties.put("CityRef", cityRef);

            List<DeliveryLocationDto> warehouses = makeApiCall("Address", "getWarehouses", methodProperties);

            if (!warehouses.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(warehouses), Duration.ofHours(12));
            }
            return warehouses;
        } catch (Exception e) {
            log.error("Error retrieving warehouses: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    private List<DeliveryLocationDto> makeApiCall(String modelName, String calledMethod, Map<String, Object> methodProperties) throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apiKey", apiKey);
        requestBody.put("modelName", modelName);
        requestBody.put("calledMethod", calledMethod);
        requestBody.put("methodProperties", methodProperties);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String responseRaw = restTemplate.postForObject(apiUrl, entity, String.class);

        JsonNode rootNode = objectMapper.readTree(responseRaw);
        JsonNode dataNode = rootNode.path("data");

        List<DeliveryLocationDto> result = new ArrayList<>();
        if (dataNode.isArray()) {
            for (JsonNode node : dataNode) {
                result.add(DeliveryLocationDto.builder()
                        .ref(node.path("Ref").asText())
                        .name(node.path("Description").asText())
                        .build());
            }
        }
        return result;
    }
}