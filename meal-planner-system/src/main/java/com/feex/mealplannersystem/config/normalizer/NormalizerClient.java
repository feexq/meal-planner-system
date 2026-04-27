package com.feex.mealplannersystem.config.normalizer;

import com.feex.mealplannersystem.config.normalizer.dto.response.NormalizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class NormalizerClient {

    private final WebClient webClient;

    public NormalizerClient(@Value("${normalizer.url}") String normalizerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(normalizerUrl)
                .build();
    }

    public String normalize(String rawName) {
        return webClient.post()
                .uri("/normalize")
                .bodyValue(Map.of("raw_name", rawName))
                .retrieve()
                .bodyToMono(NormalizeResponse.class)
                .map(NormalizeResponse::normalizedName)
                .block();
    }
}
