package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.mealplan.AdaptiveDtos;
import com.feex.mealplannersystem.dto.mealplan.GenerateFinalResponse;
import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos;
import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.MealPlanFinalizeService;
import com.feex.mealplannersystem.mealplan.service.MealPlanService;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.service.impl.FoodLogServiceImpl;
import com.feex.mealplannersystem.service.impl.MealSwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanFinalizeService finalizeService;
    private final FoodLogServiceImpl foodLogService;
    private final MealSwapService mealSwapService;

    @PostMapping("/generate")
    public ResponseEntity<WeeklyMealPlanDto> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        WeeklyMealPlanDto plan = mealPlanService.generateForUser(userDetails.getUsername());
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/{planId}/swap-slot/{slotId}")
    public ResponseEntity<AdaptiveDtos.SlotStatusDto> swapSlot(
            @PathVariable Long planId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails) {

        MealPlanSlotEntity updatedSlot = mealSwapService.swapSlotWithDynamicLimits(
                planId, slotId, userDetails.getUsername());

        AdaptiveDtos.SlotStatusDto responseDto = AdaptiveDtos.SlotStatusDto.builder()
                .slotId(updatedSlot.getId())
                .mealType(updatedSlot.getMealType())
                .recipeId(updatedSlot.getRecipeId())
                .recipeName(updatedSlot.getRecipeName())
                .targetCalories(updatedSlot.getTargetCalories())
                .actualCalories(updatedSlot.getActualCalories())
                .status(updatedSlot.getStatus() != null ? updatedSlot.getStatus().name() : null)
                .eatenAt(updatedSlot.getEatenAt())
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/generate/final")
    public ResponseEntity<GenerateFinalResponse> generateFinal(
            @AuthenticationPrincipal UserDetails userDetails) {

        MealPlanFinalizeService.SavedPlanResult result = finalizeService.generateAndFinalize(userDetails.getUsername());
        return ResponseEntity.ok(new GenerateFinalResponse(
                result.getPlanId(), result.getFinalizedPlan()));
    }
    @PostMapping("/generate/{email}")
    public ResponseEntity<WeeklyMealPlanDto> generateForUser(@PathVariable String email) {
        return ResponseEntity.ok(mealPlanService.generateForUser(email));
    }

    @PostMapping("/generate/final/{email}")
    public ResponseEntity<GenerateFinalResponse> generateFinalForUser(@PathVariable String email) {
        MealPlanFinalizeService.SavedPlanResult result = finalizeService.generateAndFinalize(email);
        return ResponseEntity.ok(new GenerateFinalResponse(
                result.getPlanId(), result.getFinalizedPlan()));
    }

    @PostMapping("/{planId}/log-food")
    public ResponseEntity<AdaptiveDtos.LogFoodResponse> logFood(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @RequestBody AdaptiveDtos.LogFoodRequest request) {

        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.logFood(userId, planId, request));
    }

    @PostMapping("/{planId}/mark-eaten")
    public ResponseEntity<AdaptiveDtos.AdaptedPlanResponse> markEaten(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId,
            @RequestBody AdaptiveDtos.MarkSlotEatenRequest request) {

        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.markSlotEaten(userId, planId, request));
    }

    @GetMapping("/{planId}/status")
    public ResponseEntity<AdaptiveDtos.PlanStatusDto> status(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long planId) {

        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.getPlanStatus(userId, planId));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.feex.mealplannersystem.repository.entity.auth.UserEntity u)
            return u.getId();
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Cannot resolve user ID");
        }
    }
}
