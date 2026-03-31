package com.feex.mealplannersystem.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Місто доставки є обов'язковим")
    private String npCityRef;

    @NotBlank(message = "Відділення доставки є обов'язковим")
    private String npWarehouseRef;
}