package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.profile.*;
import com.feex.mealplannersystem.dto.profile.achievement.AchievementResponse;
import com.feex.mealplannersystem.dto.profile.statistic.*;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import com.feex.mealplannersystem.service.impl.AchievementServiceImpl;
import com.feex.mealplannersystem.service.impl.StreakService;
import com.feex.mealplannersystem.service.impl.UserProfileServiceImpl;
import com.feex.mealplannersystem.service.impl.WeightServiceImpl;
import com.feex.mealplannersystem.service.impl.UserStatisticsServiceImpl;
import com.feex.mealplannersystem.service.mapper.WeightMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileServiceImpl profileService;
    private final WeightServiceImpl weightService;
    private final StreakService streakService;
    private final UserStatisticsServiceImpl statisticsService;
    private final AchievementServiceImpl achievementService;
    private final WeightMapper weightMapper;

    @GetMapping("/me")
    public ResponseEntity<ProfileSummaryResponse> getProfile(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(profileService.getProfileSummary(user));
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        profileService.updateProfile(user, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarResponse> uploadAvatar(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        String url = profileService.uploadAvatar(user, file);
        return ResponseEntity.ok(new AvatarResponse(url));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal UserEntity user
    ) {
        profileService.deleteAvatar(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/weight")
    public ResponseEntity<WeightResponse> logWeight(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LogWeightRequest request
    ) {
        WeightHistoryEntity entry = weightService.logWeight(
                user, request.getWeightKg(), request.getNote());
        return ResponseEntity.ok(weightMapper.toResponse(entry));
    }

    @PostMapping("/weight/{date}")
    public ResponseEntity<WeightResponse> logWeightForDate(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody LogWeightRequest request
    ) {
        WeightHistoryEntity entry = weightService.logWeightForDate(
                user, request.getWeightKg(), request.getNote(), date);
        return ResponseEntity.ok(weightMapper.toResponse(entry));
    }

    @GetMapping("/weight/history")
    public ResponseEntity<List<WeightResponse>> getWeightHistory(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int limit
    ) {
        List<WeightHistoryEntity> history = (from != null && to != null)
                ? weightService.getHistoryRange(user.getId(), from, to)
                : weightService.getRecentHistory(user.getId(), limit);

        return ResponseEntity.ok(history.stream().map(weightMapper::toResponse).toList());
    }

    @DeleteMapping("/weight/{date}")
    public ResponseEntity<Void> deleteWeightEntry(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        boolean deleted = weightService.deleteEntry(user.getId(), date);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakResponse> getStreak(
            @AuthenticationPrincipal UserEntity user
    ) {
        var meta = streakService.getStreak(user.getId());
        return ResponseEntity.ok(StreakResponse.builder()
                .currentStreak(meta.getCurrentStreak())
                .longestStreak(meta.getLongestStreak())
                .totalActiveDays(meta.getTotalActiveDays())
                .freezesAvailable(meta.getFreezesAvailable())
                .freezesUsedThisMonth(meta.getFreezesUsedThisMonth())
                .streakType(meta.getStreakType())
                .lastActiveDate(meta.getLastActiveDate())
                .build());
    }

    @PutMapping("/streak/type")
    public ResponseEntity<Void> changeStreakType(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody ChangeStreakTypeRequest request
    ) {
        streakService.changeStreakType(user, request.getStreakType());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/statistics/top-recipes")
    @Operation(summary = "Get top recipes", description = "Returns top eaten recipes by user")
    public ResponseEntity<List<TopRecipeResponse>> getTopRecipes(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return ResponseEntity.ok(statisticsService.getTopRecipes(user.getId(), limit));
    }

    @GetMapping("/statistics/nutrition-heatmap")
    @Operation(summary = "Get nutrition heatmap data", description = "Returns daily nutrition summary within a date range")
    public ResponseEntity<List<NutritionHeatmapResponse>> getNutritionHeatmap(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(statisticsService.getNutritionHeatmap(user.getId(), from, to));
    }

    @GetMapping("/achievements")
    @Operation(summary = "Get user achievements", description = "Returns a list of achievements and their status")
    public ResponseEntity<List<AchievementResponse>> getAchievements(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(achievementService.getUserAchievements(user));
    }
}
