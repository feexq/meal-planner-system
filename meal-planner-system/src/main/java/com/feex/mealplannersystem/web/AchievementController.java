package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.profile.achievement.CreateAchievementRequest;
import com.feex.mealplannersystem.dto.profile.achievement.UpdateAchievementRequest;
import com.feex.mealplannersystem.repository.entity.profile.AchievementEntity;
import com.feex.mealplannersystem.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievements")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @Operation(summary = "Get all achievements (Admin)")
    public ResponseEntity<List<AchievementEntity>> getAll() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @PostMapping
    @Operation(summary = "Create an achievement (Admin)")
    public ResponseEntity<AchievementEntity> create(@RequestBody @Valid CreateAchievementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achievementService.createAchievement(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an achievement (Admin)")
    public ResponseEntity<AchievementEntity> update(@PathVariable Long id, @RequestBody @Valid UpdateAchievementRequest request) {
        return ResponseEntity.ok(achievementService.updateAchievement(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an achievement (Admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }
}
