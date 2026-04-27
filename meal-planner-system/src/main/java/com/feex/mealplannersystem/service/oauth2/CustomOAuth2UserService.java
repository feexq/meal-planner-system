package com.feex.mealplannersystem.service.oauth2;

import com.feex.mealplannersystem.common.user.AuthProvider;
import com.feex.mealplannersystem.common.user.Role;
import com.feex.mealplannersystem.repository.UserRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.security.oauth2.CustomOAuth2User;
import com.feex.mealplannersystem.security.oauth2.GithubOAuth2UserInfo;
import com.feex.mealplannersystem.security.oauth2.GoogleOAuth2UserInfo;
import com.feex.mealplannersystem.security.oauth2.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
            String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
            OAuth2UserInfo userInfo;

            if (registrationId.equalsIgnoreCase("google")) {
                userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            } else if (registrationId.equalsIgnoreCase("github")) {
                String email = getGithubEmail(oAuth2UserRequest.getAccessToken().getTokenValue(), oAuth2User);
                log.info("GitHub email отримано: {}", email);
                userInfo = new GithubOAuth2UserInfo(oAuth2User.getAttributes(), email);
            } else {
                throw new OAuth2AuthenticationException("Провайдер " + registrationId + " не підтримується");
            }

            UserEntity user = processOAuth2User(registrationId, userInfo);
            return new CustomOAuth2User(user, oAuth2User.getAttributes());

        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2 помилка: {}", e.getError(), e); // <-- покаже реальну причину
            throw e;
        } catch (Exception e) {
            log.error("Несподівана помилка під час OAuth2: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error"), e.getMessage(), e);
        }
    }

    private String getGithubEmail(String accessToken, OAuth2User oAuth2User) {
        String email = (String) oAuth2User.getAttributes().get("email");
        if (email != null && !email.isBlank()) return email;

        RestClient restClient = RestClient.create();
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null) throw new OAuth2AuthenticationException("Не вдалося отримати email з GitHub");

            return emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> (String) e.get("email"))
                    .findFirst()
                    .orElseThrow(() -> new OAuth2AuthenticationException("GitHub акаунт не має підтвердженого email"));
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Помилка отримання email з GitHub: " + e.getMessage());
        }
    }

    private UserEntity processOAuth2User(String providerName, OAuth2UserInfo userInfo) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(userInfo.getEmail());
        UserEntity user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = UserEntity.builder()
                    .firstName(userInfo.getName())
                    .email(userInfo.getEmail())
                    .provider(AuthProvider.valueOf(providerName.toUpperCase()))
                    .providerId(userInfo.getId())
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            user = userRepository.save(user);
        }

        return user;
    }
}
