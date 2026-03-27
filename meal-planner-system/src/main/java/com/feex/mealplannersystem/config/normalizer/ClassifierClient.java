package com.feex.mealplannersystem.config.normalizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class ClassifierClient {

    private final WebClient webClient;

    public ClassifierClient(@Value("${normalizer.url}") String normalizerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(normalizerUrl)
                .build();
    }

    public ClassifyResponse classify(List<String> ingredients) {
        return webClient.post()
                .uri("/classify")
                .bodyValue(Map.of("ingredients", ingredients))
                .retrieve()
                .bodyToMono(ClassifyResponse.class)
                .block();
    }

    public record ClassifyResponse(
            @JsonProperty("results") Map<String, Map<String, String>> results,
            @JsonProperty("failed") List<String> failed
    ) {}
}
