package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.user.UserPreferenceRequest;
import com.feex.mealplannersystem.dto.user.UserPreferenceResponse;
import com.feex.mealplannersystem.repository.UserRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserPreferenceService userPreferenceService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserEntity user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole().name());
        response.put("provider", user.getProvider().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/preference")
    public ResponseEntity<UserPreferenceResponse> submit(
            @RequestBody @Valid UserPreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPreferenceService.submitPreference(userDetails.getUsername(), request));
    }

    @PutMapping("/preference")
    public ResponseEntity<UserPreferenceResponse> update(
            @RequestBody @Valid UserPreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userPreferenceService.updatePreference(userDetails.getUsername(), request));
    }

    @GetMapping("/preference")
    public ResponseEntity<UserPreferenceResponse> get(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userPreferenceService.getPreference(userDetails.getUsername()));
    }

    @GetMapping("/preference/exists")
    public ResponseEntity<Boolean> exists(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userPreferenceService.hasPreference(userDetails.getUsername()));
    }

    @DeleteMapping("/preference")
    public ResponseEntity<UserPreferenceResponse> delete(@AuthenticationPrincipal UserDetails userDetails){
        userPreferenceService.deletePreference(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
