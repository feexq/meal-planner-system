package com.feex.mealplannersystem.config.normalizer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NormalizeResponse(
        @JsonProperty("raw_name") String rawName,
        @JsonProperty("normalized_name") String normalizedName
) {}

