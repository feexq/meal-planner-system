package service;

import java.util.*;

public class ClassifiedIngredientsServiceasdas {

    private final Map<String, Map<String, String>> tagsMap;

    private final Map<String, String> normMap;

    public ClassifiedIngredientsService(Map<String, String> normMap,
            Map<String, Map<String, String>> tagsMap) {
        this.normMap = normMap != null ? normMap : Collections.emptyMap();
        this.tagsMap = tagsMap != null ? tagsMap : Collections.emptyMap();
    }

    private String normalize(String rawIngredient) {
        String lower = rawIngredient.toLowerCase().trim();
        String mapped = normMap.get(lower);
        return mapped != null ? mapped.toLowerCase().trim() : lower;
    }

    private String getStatus(String rawIngredient, String condition) {
        String normalized = normalize(rawIngredient);
        Map<String, String> condTags = tagsMap.get(normalized);
        if (condTags == null)
            return null;
        return condTags.get(condition.toLowerCase());
    }

    public boolean hasForbiddenIngredient(List<String> recipeIngredients, String condition) {
        if (recipeIngredients == null || condition == null)
            return false;
        for (String ing : recipeIngredients) {
            if ("hard_forbidden".equals(getStatus(ing, condition)))
                return true;
        }
        return false;
    }

    public boolean hasSoftForbiddenIngredient(List<String> recipeIngredients, String condition) {
        if (recipeIngredients == null || condition == null)
            return false;
        for (String ing : recipeIngredients) {
            if ("soft_forbidden".equals(getStatus(ing, condition)))
                return true;
        }
        return false;
    }

    public int countSoftForbiddenMatches(List<String> recipeIngredients, String condition) {
        if (recipeIngredients == null || condition == null)
            return 0;
        Set<String> matched = new LinkedHashSet<>();
        for (String ing : recipeIngredients) {
            if ("soft_forbidden".equals(getStatus(ing, condition))) {
                matched.add(normalize(ing));
            }
        }
        return matched.size();
    }

    public List<String> getHardForbiddenMatches(List<String> recipeIngredients, String condition) {
        if (recipeIngredients == null || condition == null)
            return Collections.emptyList();
        Set<String> seenNorm = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String ing : recipeIngredients) {
            if ("hard_forbidden".equals(getStatus(ing, condition))) {
                String norm = normalize(ing);
                if (seenNorm.add(norm))
                    result.add(ing);
            }
        }
        return result;
    }

    public List<String> getSoftForbiddenMatches(List<String> recipeIngredients, String condition) {
        if (recipeIngredients == null || condition == null)
            return Collections.emptyList();
        Set<String> seenNorm = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String ing : recipeIngredients) {
            if ("soft_forbidden".equals(getStatus(ing, condition))) {
                String norm = normalize(ing);
                if (seenNorm.add(norm))
                    result.add(ing);
            }
        }
        return result;
    }

    public Set<String> getConditionNames() {
        Set<String> conditions = new LinkedHashSet<>();
        for (Map<String, String> tags : tagsMap.values()) {
            conditions.addAll(tags.keySet());
        }
        return conditions;
    }
}