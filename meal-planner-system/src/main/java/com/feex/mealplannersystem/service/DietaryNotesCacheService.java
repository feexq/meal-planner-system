package com.feex.mealplannersystem.service;

import java.util.List;

public interface DietaryNotesCacheService {
    void putNotes(Long userId, Long recipeId, List<String> notes);
    List<String> getNotes(Long userId, Long recipeId);
}
