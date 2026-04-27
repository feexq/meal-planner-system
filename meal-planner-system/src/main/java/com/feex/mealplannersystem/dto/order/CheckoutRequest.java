package com.feex.mealplannersystem.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "City is required")
    private String npCityRef;

    @NotBlank(message = "Warehouse is required")
    private String npWarehouseRef;
}