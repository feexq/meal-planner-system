package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.user.UserPreferenceRequest;
import com.feex.mealplannersystem.dto.user.UserPreferenceResponse;
import com.feex.mealplannersystem.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preference")
@RequiredArgsConstructor
public class PreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping()
    public ResponseEntity<UserPreferenceResponse> submit(
            @RequestBody @Valid UserPreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPreferenceService.submitPreference(userDetails.getUsername(), request));
    }

    @PutMapping()
    public ResponseEntity<UserPreferenceResponse> update(
            @RequestBody @Valid UserPreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userPreferenceService.updatePreference(userDetails.getUsername(), request));
    }

    @GetMapping()
    public ResponseEntity<UserPreferenceResponse> get(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userPreferenceService.getPreference(userDetails.getUsername()));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userPreferenceService.hasPreference(userDetails.getUsername()));
    }

    @DeleteMapping()
    public ResponseEntity<UserPreferenceResponse> delete(@AuthenticationPrincipal UserDetails userDetails){
        userPreferenceService.deletePreference(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
