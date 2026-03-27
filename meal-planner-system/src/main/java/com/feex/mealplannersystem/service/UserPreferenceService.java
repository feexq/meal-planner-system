package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.user.UserPreferenceRequest;
import com.feex.mealplannersystem.dto.user.UserPreferenceResponse;

public interface UserPreferenceService {
    UserPreferenceResponse submitPreference(String email, UserPreferenceRequest request);
    UserPreferenceResponse updatePreference(String email, UserPreferenceRequest request);
    UserPreferenceResponse getPreference(String email);
    void deletePreference(String email);
    boolean hasPreference(String email);
}
