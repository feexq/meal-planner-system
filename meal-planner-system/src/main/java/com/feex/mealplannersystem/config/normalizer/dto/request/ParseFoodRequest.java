package com.feex.mealplannersystem.config.normalizer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParseFoodRequest {
    private String text;
    private String language = "auto";

    public ParseFoodRequest(String text) { this.text = text; }
}
