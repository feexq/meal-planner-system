package com.feex.mealplannersystem.domain.user;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class User {
    Long id;
    String username;
    String password;
}
