package com.feex.mealplannersystem.dto.profile.statistic;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class WeightResponse {
    Long id;
    double weightKg;
    LocalDate recordedDate;
    String note;
}
