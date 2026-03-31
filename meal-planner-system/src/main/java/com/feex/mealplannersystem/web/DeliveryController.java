package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.delivery.DeliveryLocationDto;
import com.feex.mealplannersystem.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/cities")
    public ResponseEntity<List<DeliveryLocationDto>> searchCities(
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(deliveryService.searchCities(name));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<DeliveryLocationDto>> getWarehouses(
            @RequestParam String cityRef) {
        return ResponseEntity.ok(deliveryService.getWarehouses(cityRef));
    }
}