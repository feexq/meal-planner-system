package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.mealplan.dto.FilterDebugDto;
import com.feex.mealplannersystem.mealplan.service.MealPlanDebugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/meal-plan/debug")
@RequiredArgsConstructor
public class MealPlanDebugController {

    private final MealPlanDebugService debugService;


    @GetMapping("/filter/{email}")
    public ResponseEntity<FilterDebugDto> filterDebug(
            @PathVariable String email,
            @RequestParam(defaultValue = "all") String slot) {

        return ResponseEntity.ok(debugService.debugFilter(email, slot));
    }

    @GetMapping("/filter/{email}/all")
    public ResponseEntity<Map<String, FilterDebugDto>> filterDebugAll(@PathVariable String email) {
        return ResponseEntity.ok(debugService.debugAllSlots(email));
    }
}
