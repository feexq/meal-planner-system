package com.feex.mealplannersystem.dto.dietary;

import com.feex.mealplannersystem.common.DietaryTagStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDietaryTagsRequest {
    private Map<String, DietaryTagStatus> tags;
}
