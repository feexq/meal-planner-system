package com.feex.mealplannersystem.security.oauth2;

import java.util.Map;

public class GithubOAuth2UserInfo extends OAuth2UserInfo {
    private final String email;

    public GithubOAuth2UserInfo(Map<String, Object> attributes, String email) {
        super(attributes);
        this.email = email;
    }

    @Override public String getId() { return String.valueOf(attributes.get("id")); }
    @Override public String getName() { return (String) attributes.get("name"); }
    @Override public String getEmail() { return email; }
}
