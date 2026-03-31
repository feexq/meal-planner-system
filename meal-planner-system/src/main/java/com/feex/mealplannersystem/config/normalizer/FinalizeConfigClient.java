package com.feex.mealplannersystem.config.normalizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FinalizeConfigClient {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024));
    }
}
