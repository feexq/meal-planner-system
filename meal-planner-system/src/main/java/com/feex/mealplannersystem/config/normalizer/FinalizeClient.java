package com.feex.mealplannersystem.config.normalizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto;
import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos.FinalizedMealPlanDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizeClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${normalizer.url}")
    private String nlpServiceUrl;

    @Value("${normalizer.service.timeout-seconds}")
    private int timeoutSeconds;


    @Getter
    @AllArgsConstructor
    public static class ParseFoodRequest {
        private String text;
        private String language = "auto";

        public ParseFoodRequest(String text) { this.text = text; }
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class ParsedFoodItem {
        private String name;
        private String original;
        @JsonProperty("quantity_description")
        private String quantityDescription;
        private double calories;
        @JsonProperty("protein_g")
        private double proteinG;
        @JsonProperty("carbs_g")
        private double carbsG;
        @JsonProperty("fat_g")
        private double fatG;
        private String confidence;
        @JsonProperty("from_cache")
        private boolean fromCache;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class ParseFoodResponse {
        private List<ParsedFoodItem> items;
        @JsonProperty("total_calories")
        private double totalCalories;
        @JsonProperty("total_protein_g")
        private double totalProteinG;
        @JsonProperty("total_carbs_g")
        private double totalCarbsG;
        @JsonProperty("total_fat_g")
        private double totalFatG;
        @JsonProperty("parse_note")
        private String parseNote;
    }


    public FinalizedMealPlanDto finalize(FinalizeRequestDto request) {
        log.info("Calling Python /finalize for user={}", request.getUserId());

        try {
            return webClientBuilder
                    .baseUrl(nlpServiceUrl)
                    .build()
                    .post()
                    .uri("/finalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FinalizedMealPlanDto.class)
                    .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                    .block();

        } catch (WebClientResponseException e) {
            log.error("Python /finalize returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MealPlanFinalizationException("LLM finalization failed: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Python /finalize call failed: {}", e.getMessage());
            throw new MealPlanFinalizationException("LLM finalization failed: " + e.getMessage(), e);
        }
    }

    public static class MealPlanFinalizationException extends RuntimeException {
        public MealPlanFinalizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public ParseFoodResponse parseFood(String foodText) {
        log.info("Calling Python /parse-food: \"{}\"", foodText);
        try {
            return webClientBuilder
                    .baseUrl(nlpServiceUrl)
                    .build()
                    .post()
                    .uri("/parse-food")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ParseFoodRequest(foodText))
                    .retrieve()
                    .bodyToMono(ParseFoodResponse.class)
                    .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception e) {
            log.error("Python /parse-food failed: {}", e.getMessage());
            throw new MealPlanFinalizationException("Food parsing failed: " + e.getMessage(), e);
        }
    }
}
