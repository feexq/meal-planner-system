package com.feex.mealplannersystem.domain.user;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserProfile {
    Long id;
    String firstName;
    String lastName;
    String email;
    User user;
}
