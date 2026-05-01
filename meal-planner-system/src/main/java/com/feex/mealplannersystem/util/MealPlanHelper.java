package com.feex.mealplannersystem.util;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class MealPlanHelper {
    public Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof UserEntity u)
            return u.getId();
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Cannot resolve user ID");
        }
    }
}
