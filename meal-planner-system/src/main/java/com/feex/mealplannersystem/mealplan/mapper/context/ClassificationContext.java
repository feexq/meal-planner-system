package com.feex.mealplannersystem.mealplan.mapper.context;

import java.util.*;

public class ClassificationContext {

    private final Map<String, String> normMap;
    private final Map<String, Map<String, String>> tagsMap;

    public ClassificationContext(Map<String, String> normMap, Map<String, Map<String, String>> tagsMap) {
        this.normMap = normMap;
        this.tagsMap = tagsMap;
    }

    public String normalize(String raw) {
        String lower = raw.toLowerCase().trim();
        return normMap.getOrDefault(lower, lower);
    }

    private String getStatus(String rawIngredient, String condition) {
        Map<String, String> condTags = tagsMap.get(normalize(rawIngredient));
        if (condTags == null) return null;
        return condTags.get(condition.toLowerCase());
    }

    public boolean hasForbiddenIngredient(List<String> ingredients, String condition) {
        if (ingredients == null || condition == null) return false;
        for (String ing : ingredients)
            if ("hard_forbidden".equals(getStatus(ing, condition))) return true;
        return false;
    }

    public boolean isNotSuitableFor(List<String> ingredients, String diet) {
        return hasForbiddenIngredient(ingredients, diet);
    }

    public List<String> getSoftForbiddenMatches(List<String> ingredients, String condition) {
        if (ingredients == null || condition == null) return Collections.emptyList();
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String ing : ingredients) {
            if ("soft_forbidden".equals(getStatus(ing, condition))) {
                String norm = normalize(ing);
                if (seen.add(norm)) result.add(ing);
            }
        }
        return result;
    }
}