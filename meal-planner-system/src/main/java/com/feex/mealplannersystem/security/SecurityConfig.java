package com.feex.mealplannersystem.security;

import com.feex.mealplannersystem.config.jwt.JwtAuthenticationEntryPoint;
import com.feex.mealplannersystem.config.jwt.JwtAuthenticationFilter;
import com.feex.mealplannersystem.config.jwt.JwtTokenProvider;
import com.feex.mealplannersystem.service.CustomUserDetailService;
import com.feex.mealplannersystem.service.oauth2.CustomOAuth2UserService;
import com.feex.mealplannersystem.service.oauth2.OAuth2AuthenticationSuccessHandler;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/v3/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/delivery/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ingredients").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ingredients/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ingredients/{id}/dietary-tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cart/add-recipe/{recipeId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/by-ingredient/{ingredientId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags-recipes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags-recipes/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dietary-conditions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ingredients/{id}/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cart").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/cart").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cart/items").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/cart/items/{ingredientId}").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/cart/items/{ingredientId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/search/by-ingredients").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/meal-plans/**").authenticated()
                        .requestMatchers("/api/nutrition/**").authenticated()
                        .requestMatchers("/api/food-logs/**").authenticated()
                        .requestMatchers("/api/analytics/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/ingredients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ingredients/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ingredients/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ingredients/{id}/dietary-tags").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tags").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tags-recipes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tags-recipes/{id}").hasRole("ADMIN")
                        .anyRequest().authenticated()
                ).oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

}
