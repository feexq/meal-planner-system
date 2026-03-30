package com.feex.mealplannersystem.domain.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EstimatedDailyMacros {
    private int proteinG;
    private int carbsG;
    private int fatG;
    private int proteinTargetG;
    private int proteinMinG;
    private int proteinMaxG;
    private int proteinCoveragePercent;
    private String proteinStatus;
    private boolean criticalProteinWarning;

    private int fatTargetG;
    private int fatMinG;
    private int fatMaxG;
    private int carbsTargetG;
    private int carbsAbsoluteMinG;
    private boolean lowCarbWarning;

    private int carbsCoveragePercent;
    private String carbsStatus;
    private int fatCoveragePercent;
    private String fatStatus;

    public EstimatedDailyMacros(int proteinG, int carbsG, int fatG,
                                int proteinTargetG, int proteinMinG, int proteinMaxG,
                                int fatTargetG, int fatMinG, int fatMaxG,
                                int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
                                int proteinCoveragePercent, String proteinStatus,
                                int extraCoveragePercent, String extraStatus1, String extraStatus2) {
        this(proteinG, carbsG, fatG, proteinTargetG, proteinMinG, proteinMaxG,
                fatTargetG, fatMinG, fatMaxG, carbsTargetG, carbsAbsoluteMinG,
                lowCarbWarning, proteinCoveragePercent, proteinStatus);

        this.carbsCoveragePercent = extraCoveragePercent;
        this.carbsStatus = extraStatus1;
        this.fatStatus = extraStatus2;
    }

    public EstimatedDailyMacros(int proteinG, int carbsG, int fatG,
                                int proteinTargetG, int proteinMinG, int proteinMaxG,
                                int fatTargetG, int fatMinG, int fatMaxG,
                                int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
                                int proteinCoveragePercent, String proteinStatus,
                                int carbsCoveragePercent, String carbsStatus,
                                int fatCoveragePercent, String fatStatus) {
        this(proteinG, carbsG, fatG, proteinTargetG, proteinMinG, proteinMaxG,
                fatTargetG, fatMinG, fatMaxG, carbsTargetG, carbsAbsoluteMinG,
                lowCarbWarning, proteinCoveragePercent, proteinStatus);
        this.carbsCoveragePercent = carbsCoveragePercent;
        this.carbsStatus = carbsStatus;
        this.fatCoveragePercent = fatCoveragePercent;
        this.fatStatus = fatStatus;
    }

    public EstimatedDailyMacros(int proteinG, int carbsG, int fatG,
                                int proteinTargetG, int proteinMinG, int proteinMaxG,
                                int fatTargetG, int fatMinG, int fatMaxG,
                                int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
                                int proteinCoveragePercent, String proteinStatus) {
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.proteinTargetG = proteinTargetG;
        this.proteinMinG = proteinMinG;
        this.proteinMaxG = proteinMaxG;
        this.fatTargetG = fatTargetG;
        this.fatMinG = fatMinG;
        this.fatMaxG = fatMaxG;
        this.carbsTargetG = carbsTargetG;
        this.carbsAbsoluteMinG = carbsAbsoluteMinG;
        this.lowCarbWarning = lowCarbWarning;
        this.proteinCoveragePercent = proteinCoveragePercent;
        this.proteinStatus = proteinStatus;
        this.criticalProteinWarning = "CRITICAL".equals(proteinStatus);
    }
}
