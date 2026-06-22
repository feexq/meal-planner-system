package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.mealplan.request.LogFoodRequest;
import com.feex.mealplannersystem.dto.mealplan.request.MarkSlotEatenRequest;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.dto.mealplan.response.GenerateFinalResponse;
import com.feex.mealplannersystem.dto.mealplan.response.LogFoodResponse;
import com.feex.mealplannersystem.dto.mealplan.status.PlanStatusDto;
import com.feex.mealplannersystem.dto.mealplan.status.SlotStatusDto;
import com.feex.mealplannersystem.dto.mealplan.SavedPlanResult;
import com.feex.mealplannersystem.mealplan.dto.plan.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.service.MealPlanFinalizeService;
import com.feex.mealplannersystem.mealplan.service.MealPlanService;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.service.FoodLogService;
import com.feex.mealplannersystem.service.MealSwapService;
import com.feex.mealplannersystem.util.MealPlanHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.feex.mealplannersystem.service.AsyncJobTrackerService;

@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanFinalizeService finalizeService;
    private final FoodLogService foodLogService;
    private final MealPlanHelper mealPlanHelper;
    private final MealSwapService mealSwapService;
    private final AsyncJobTrackerService asyncJobTrackerService;

    @PostMapping("/generate")
    public ResponseEntity<WeeklyMealPlanDto> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        WeeklyMealPlanDto plan = mealPlanService.generateForUser(userDetails.getUsername());
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/swap-slot/{slotId}")
    public ResponseEntity<SlotStatusDto> swapSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails) {

        MealPlanSlotEntity updatedSlot = mealSwapService.swapSlotAuto(
                slotId, userDetails.getUsername());

        SlotStatusDto responseDto = SlotStatusDto.builder()
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

    @PostMapping("/swap-slot/main/{slotId}")
    public ResponseEntity<SlotStatusDto> swapMainSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails) {

        MealPlanSlotEntity updatedSlot = mealSwapService.swapMainSlot(
                slotId, userDetails.getUsername());

        SlotStatusDto responseDto = SlotStatusDto.builder()
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

    @PostMapping("/swap-slot/side/{slotId}")
    public ResponseEntity<SlotStatusDto> swapSideSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails) {

        MealPlanSlotEntity updatedSlot = mealSwapService.swapSideSlot(
                slotId, userDetails.getUsername());

        SlotStatusDto responseDto = SlotStatusDto.builder()
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

        SavedPlanResult result = finalizeService.generateAndFinalize(userDetails.getUsername());
        return ResponseEntity.ok(new GenerateFinalResponse(
                result.getPlanId(), result.getFinalizedPlan()));
    }

    @PostMapping("/generate/final/async")
    public ResponseEntity<Map<String, String>> generateFinalAsync(
            @AuthenticationPrincipal UserDetails userDetails) {

        String jobId = asyncJobTrackerService.startJob(() -> {
            SavedPlanResult result = finalizeService.generateAndFinalize(userDetails.getUsername());
            return new GenerateFinalResponse(result.getPlanId(), result.getFinalizedPlan());
        });

        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/generate/final/status/{jobId}")
    public ResponseEntity<?> getGenerateFinalStatus(@PathVariable String jobId) {
        Map<String, Object> status = asyncJobTrackerService.getJobStatus(jobId);
        if ("ERROR".equals(status.get("status"))) {
            return ResponseEntity.badRequest().body(status);
        }
        return ResponseEntity.ok(status);
    }
    @PostMapping("/generate/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeeklyMealPlanDto> generateForUser(@PathVariable String email) {
        return ResponseEntity.ok(mealPlanService.generateForUser(email));
    }

    @PostMapping("/generate/final/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenerateFinalResponse> generateFinalForUser(@PathVariable String email) {
        SavedPlanResult result = finalizeService.generateAndFinalize(email);
        return ResponseEntity.ok(new GenerateFinalResponse(
                result.getPlanId(), result.getFinalizedPlan()));
    }

    @PostMapping("/generate/final/{email}/async")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generateFinalForUserAsync(@PathVariable String email) {
        String jobId = asyncJobTrackerService.startJob(() -> {
            SavedPlanResult result = finalizeService.generateAndFinalize(email);
            return new GenerateFinalResponse(result.getPlanId(), result.getFinalizedPlan());
        });
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @PostMapping("/log-food")
    public ResponseEntity<LogFoodResponse> logFood(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LogFoodRequest request) {

        Long userId = mealPlanHelper.extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.logFood(userId, request));
    }

    @PostMapping("/mark-eaten")
    public ResponseEntity<AdaptedPlanResponse> markEaten(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MarkSlotEatenRequest request) {

        Long userId = mealPlanHelper.extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.markSlotEaten(userId, request));
    }

    @GetMapping("/status")
    public ResponseEntity<PlanStatusDto> status(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = mealPlanHelper.extractUserId(userDetails);
        return ResponseEntity.ok(foodLogService.getPlanStatus(userId));
    }


}
