package service;

import model.Ingredient;

import java.util.*;

public class IngredientLookupService {

    private final Map<String, Ingredient> exactIndex; 
    private final List<Ingredient> allIngredients; 

    private final Map<String, List<Ingredient>> tokenIndex;

    public IngredientLookupService(List<Ingredient> ingredients) {
        this.allIngredients = new ArrayList<>();
        this.exactIndex = new HashMap<>();
        this.tokenIndex = new HashMap<>();

        for (Ingredient ing : ingredients) {
            String key = ing.getName().toLowerCase().trim();
            exactIndex.put(key, ing);
            allIngredients.add(ing);

            for (String token : key.split("[\\s,]+")) {
                if (token.length() >= 3) {
                    tokenIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(ing);
                }
            }
        }
    }

    public Ingredient lookup(String recipeIngredientName) {
        String normalized = recipeIngredientName.toLowerCase().trim();

        Ingredient exact = exactIndex.get(normalized);
        if (exact != null)
            return exact;

        Set<Ingredient> candidates = new LinkedHashSet<>();
        for (String token : normalized.split("[\\s,]+")) {
            if (token.length() >= 3) {
                List<Ingredient> hits = tokenIndex.get(token);
                if (hits != null)
                    candidates.addAll(hits);
            }
        }

        Iterable<Ingredient> searchSpace = candidates.isEmpty() ? allIngredients : candidates;

        Ingredient best = null;
        int bestLen = Integer.MAX_VALUE;
        for (Ingredient ing : searchSpace) {
            String dbName = ing.getName().toLowerCase();
            if (dbName.contains(normalized) && dbName.length() < bestLen) {
                best = ing;
                bestLen = dbName.length();
            }
        }
        if (best != null)
            return best;

        int bestMatchLen = 0;
        for (Ingredient ing : searchSpace) {
            String dbName = ing.getName().toLowerCase();
            if (normalized.contains(dbName) && dbName.length() > bestMatchLen) {
                best = ing;
                bestMatchLen = dbName.length();
            }
        }
        return best; 
    }

    public IngredientLookupResult resolveRecipe(List<String> recipeIngredients) {
        Set<String> contraindications = new LinkedHashSet<>();
        Set<String> notSuitableForDiets = new LinkedHashSet<>();
        List<String> matchedIngredientNames = new ArrayList<>();
        List<String> unmatchedIngredientNames = new ArrayList<>();

        for (String recipeIngr : recipeIngredients) {
            Ingredient match = lookup(recipeIngr);
            if (match != null) {
                if (match.getContraindications() != null)
                    contraindications.addAll(match.getContraindications());
                if (match.getNotSuitableForDiets() != null)
                    notSuitableForDiets.addAll(match.getNotSuitableForDiets());
                matchedIngredientNames.add(match.getName());
            } else {
                unmatchedIngredientNames.add(recipeIngr);
            }
        }

        return new IngredientLookupResult(
                contraindications, notSuitableForDiets,
                matchedIngredientNames, unmatchedIngredientNames);
    }

    public static class IngredientLookupResult {
        private final Set<String> contraindications;
        private final Set<String> notSuitableForDiets;
        private final List<String> matchedIngredientNames;
        private final List<String> unmatchedIngredientNames;

        public IngredientLookupResult(Set<String> contraindications,
                Set<String> notSuitableForDiets,
                List<String> matchedIngredientNames,
                List<String> unmatchedIngredientNames) {
            this.contraindications = contraindications;
            this.notSuitableForDiets = notSuitableForDiets;
            this.matchedIngredientNames = matchedIngredientNames;
            this.unmatchedIngredientNames = unmatchedIngredientNames;
        }

        public Set<String> getContraindications() {
            return contraindications;
        }

        public Set<String> getNotSuitableForDiets() {
            return notSuitableForDiets;
        }

        public List<String> getMatchedIngredientNames() {
            return matchedIngredientNames;
        }

        public List<String> getUnmatchedIngredientNames() {
            return unmatchedIngredientNames;
        }

        public boolean hasContraindication(String condition) {
            for (String c : contraindications) {
                if (c.equalsIgnoreCase(condition))
                    return true;
            }
            return false;
        }

        public boolean isNotSuitableFor(String diet) {
            for (String d : notSuitableForDiets) {
                if (d.equalsIgnoreCase(diet))
                    return true;
            }
            return false;
        }
    }
}