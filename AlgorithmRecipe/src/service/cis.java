package service;

import model.ClassifiedCondition;

import java.util.*;
import java.util.regex.Pattern;

public class ClassifiedIngredientsService {

    private final Map<String, ClassifiedCondition> conditions;
    private final Map<String, Set<String>> hardForbiddenSets;
    private final Map<String, Set<String>> softForbiddenSets;
    private final Map<String, List<Pattern>> hardForbiddenPatterns;
    private final Map<String, List<Pattern>> softForbiddenPatterns;

    public ClassifiedIngredientsService(Map<String, ClassifiedCondition> conditions) {
        this.conditions = conditions != null ? conditions : new HashMap<>();
        this.hardForbiddenSets = new HashMap<>();
        this.softForbiddenSets = new HashMap<>();
        this.hardForbiddenPatterns = new HashMap<>();
        this.softForbiddenPatterns = new HashMap<>();

        for (Map.Entry<String, ClassifiedCondition> entry : this.conditions.entrySet()) {
            String cond = entry.getKey().toLowerCase();
            ClassifiedCondition data = entry.getValue();

            Set<String> hardStr = new HashSet<>();
            List<Pattern> hardPat = new ArrayList<>();
            if (data.getHardForbidden() != null) {
                for (String f : data.getHardForbidden()) {
                    String clean = f.toLowerCase().trim();
                    hardStr.add(clean);
                    hardPat.add(Pattern.compile("\\b" + Pattern.quote(clean) + "\\b"));
                }
            }
            hardForbiddenSets.put(cond, hardStr);
            hardForbiddenPatterns.put(cond, hardPat);

            Set<String> softStr = new HashSet<>();
            List<Pattern> softPat = new ArrayList<>();
            if (data.getSoftForbidden() != null) {
                for (String s : data.getSoftForbidden()) {
                    String clean = s.toLowerCase().trim();
                    softStr.add(clean);
                    softPat.add(Pattern.compile("\\b" + Pattern.quote(clean) + "\\b"));
                }
            }
            softForbiddenSets.put(cond, softStr);
            softForbiddenPatterns.put(cond, softPat);
        }
    }

    public boolean hasForbiddenIngredient(List<String> recipeIngredients, String condition) {
        String condKey = condition.toLowerCase();
        Set<String> hardForbidden = hardForbiddenSets.get(condKey);
        List<Pattern> hardPatterns = hardForbiddenPatterns.get(condKey);

        if (hardForbidden == null || hardForbidden.isEmpty())
            return false;

        for (String ingredient : recipeIngredients) {
            String ingLower = ingredient.toLowerCase().trim();
            if (hardForbidden.contains(ingLower))
                return true;
            if (hardPatterns != null) {
                for (Pattern p : hardPatterns) {
                    if (p.matcher(ingLower).find())
                        return true;
                }
            }
        }
        return false;
    }

    public boolean hasSoftForbiddenIngredient(List<String> recipeIngredients, String condition) {
        String condKey = condition.toLowerCase();
        Set<String> softForbidden = softForbiddenSets.get(condKey);
        List<Pattern> softPatterns = softForbiddenPatterns.get(condKey);

        if (softForbidden == null || softForbidden.isEmpty())
            return false;

        for (String ingredient : recipeIngredients) {
            String ingLower = ingredient.toLowerCase().trim();
            if (softForbidden.contains(ingLower))
                return true;
            if (softPatterns != null) {
                for (Pattern p : softPatterns) {
                    if (p.matcher(ingLower).find())
                        return true;
                }
            }
        }
        return false;
    }

    public int countSoftForbiddenMatches(List<String> recipeIngredients, String condition) {
        String condKey = condition.toLowerCase();
        Set<String> softForbidden = softForbiddenSets.get(condKey);
        List<Pattern> softPatterns = softForbiddenPatterns.get(condKey);

        if (softForbidden == null || softForbidden.isEmpty())
            return 0;

        Set<String> matchedIngredients = new LinkedHashSet<>();

        for (String ingredient : recipeIngredients) {
            String ingLower = ingredient.toLowerCase().trim();
            boolean matched = softForbidden.contains(ingLower);
            if (!matched && softPatterns != null) {
                for (Pattern p : softPatterns) {
                    if (p.matcher(ingLower).find()) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                matchedIngredients.add(ingLower); 
            }
        }
        return matchedIngredients.size();
    }

    public List<String> getHardForbiddenMatches(List<String> recipeIngredients, String condition) {
        String condKey = condition.toLowerCase();
        Set<String> hardForbidden = hardForbiddenSets.get(condKey);
        List<Pattern> hardPatterns = hardForbiddenPatterns.get(condKey);

        if (hardForbidden == null || hardForbidden.isEmpty())
            return new ArrayList<>();

        List<String> matches = new ArrayList<>();
        for (String ingredient : recipeIngredients) {
            String ingLower = ingredient.toLowerCase().trim();
            if (hardForbidden.contains(ingLower)) {
                matches.add(ingredient);
                continue;
            }
            if (hardPatterns != null) {
                for (Pattern p : hardPatterns) {
                    if (p.matcher(ingLower).find()) {
                        matches.add(ingredient);
                        break;
                    }
                }
            }
        }
        return matches;
    }

    public List<String> getSoftForbiddenMatches(List<String> recipeIngredients, String condition) {
        String condKey = condition.toLowerCase();
        Set<String> softForbidden = softForbiddenSets.get(condKey);
        List<Pattern> softPatterns = softForbiddenPatterns.get(condKey);

        if (softForbidden == null || softForbidden.isEmpty())
            return new ArrayList<>();

        Set<String> matchedSet = new LinkedHashSet<>();

        for (String ingredient : recipeIngredients) {
            String ingLower = ingredient.toLowerCase().trim();
            boolean matched = softForbidden.contains(ingLower);
            if (!matched && softPatterns != null) {
                for (Pattern p : softPatterns) {
                    if (p.matcher(ingLower).find()) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched)
                matchedSet.add(ingredient); 
        }
        return new ArrayList<>(matchedSet);
    }

    public Set<String> getConditionNames() {
        return hardForbiddenSets.keySet();
    }
}