package com.feex.mealplannersystem.mealplan.dto;

import com.feex.mealplannersystem.mealplan.model.RecipeModel;

import java.util.List;
import java.util.Map;

public record FilterResult(List<RecipeModel> recipes, String filteringNote,
                           Map<String, Integer> eliminationCounts) {}
