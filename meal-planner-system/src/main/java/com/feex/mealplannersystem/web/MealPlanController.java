package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos;
import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.service.MealPlanFinalizeService;
import com.feex.mealplannersystem.mealplan.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanFinalizeService finalizeService;

    @PostMapping("/generate")
    public ResponseEntity<WeeklyMealPlanDto> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        WeeklyMealPlanDto plan = mealPlanService.generateForUser(userDetails.getUsername());
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/generate/final")
    public ResponseEntity<FinalizedMealPlanDtos.FinalizedMealPlanDto> generateFinal(
            @AuthenticationPrincipal UserDetails userDetails) {

        FinalizedMealPlanDtos.FinalizedMealPlanDto plan = finalizeService.generateAndFinalize(userDetails.getUsername());
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/generate/{email}")
    public ResponseEntity<WeeklyMealPlanDto> generateForUser(@PathVariable String email) {
        return ResponseEntity.ok(mealPlanService.generateForUser(email));
    }

    @PostMapping("/generate/final/{email}")
    public ResponseEntity<FinalizedMealPlanDtos.FinalizedMealPlanDto> generateFinalForUser(@PathVariable String email) {
        return ResponseEntity.ok(finalizeService.generateAndFinalize(email));
    }
}
