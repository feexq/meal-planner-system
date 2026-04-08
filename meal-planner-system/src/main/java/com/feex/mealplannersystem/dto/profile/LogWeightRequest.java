package com.feex.mealplannersystem.dto.profile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LogWeightRequest {
    @NotNull
    @DecimalMin("1.0") @DecimalMax("500.0")
    private Double weightKg;

    @Size(max = 200)
    private String note;
}
