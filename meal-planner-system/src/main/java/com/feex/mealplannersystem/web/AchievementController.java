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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievements")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<AchievementEntity>> getAll() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AchievementEntity> create(@RequestBody @Valid CreateAchievementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achievementService.createAchievement(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AchievementEntity> update(@PathVariable Long id, @RequestBody @Valid UpdateAchievementRequest request) {
        return ResponseEntity.ok(achievementService.updateAchievement(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }
}
