package com.feex.mealplannersystem.config.normalizer;

import com.feex.mealplannersystem.config.normalizer.dto.request.SwapSideRequest;
import com.feex.mealplannersystem.config.normalizer.dto.response.SwapSideResponse;
import com.feex.mealplannersystem.config.normalizer.dto.response.ParseFoodResponse;
import com.feex.mealplannersystem.config.normalizer.dto.request.ParseFoodRequest;
import com.feex.mealplannersystem.mealplan.dto.finalize.FinalizeRequestDto;
import com.feex.mealplannersystem.dto.mealplan.FinalizedMealPlanDto;
import com.feex.mealplannersystem.service.exception.MealPlanFinalizationException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizeClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${normalizer.url}")
    private String nlpServiceUrl;

    @Value("${normalizer.service.timeout-seconds}")
    private int timeoutSeconds;

     public SwapSideResponse swapSide(SwapSideRequest request) {
         return webClientBuilder
                 .baseUrl(nlpServiceUrl)
                 .build()
                 .post()
                 .uri("/swap-slot/side")
                 .bodyValue(request)
                 .retrieve()
                 .bodyToMono(SwapSideResponse.class)
                 .timeout(java.time.Duration.ofSeconds(90))
                 .block();
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
            throw new MealPlanFinalizationException(e);

        } catch (Exception e) {
            log.error("Python /finalize call failed: {}", e.getMessage());
            throw new MealPlanFinalizationException(e);
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
            throw new MealPlanFinalizationException(e);
        }
    }
}
