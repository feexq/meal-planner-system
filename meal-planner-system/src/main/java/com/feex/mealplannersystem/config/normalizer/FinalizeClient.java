package com.feex.mealplannersystem.config.normalizer;

import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto;
import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos.FinalizedMealPlanDto;
import lombok.RequiredArgsConstructor;
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
}
