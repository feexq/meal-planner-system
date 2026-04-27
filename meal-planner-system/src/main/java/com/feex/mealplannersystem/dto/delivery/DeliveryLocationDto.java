package com.feex.mealplannersystem.dto.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLocationDto {
    @NotNull(message = "Ref is required")
    private String ref;

    @NotNull(message = "Name is required")
    private String name;
}