package com.feex.mealplannersystem.mealplan.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstimatedDailyMacrosDto {
    private int proteinG;
    private int carbsG;
    private int fatG;
    private int proteinTargetG;
    private int proteinMinG;
    private int proteinMaxG;
    private int fatTargetG;
    private int fatMinG;
    private int fatMaxG;
    private int carbsTargetG;
    private int carbsAbsoluteMinG;
    private boolean lowCarbWarning;
    private int proteinCoveragePercent;
    private String proteinStatus;
    private boolean criticalProteinWarning;
    private int carbsCoveragePercent;
    private String carbsStatus;
    private int fatCoveragePercent;
    private String fatStatus;
}
