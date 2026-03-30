package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
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

    @PostMapping("/generate")
    public ResponseEntity<WeeklyMealPlanDto> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        WeeklyMealPlanDto plan = mealPlanService.generateForUser(userDetails.getUsername());
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/generate/{email}")
    public ResponseEntity<WeeklyMealPlanDto> generateForUser(@PathVariable String email) {
        WeeklyMealPlanDto plan = mealPlanService.generateForUser(email);
        return ResponseEntity.ok(plan);
    }
}
